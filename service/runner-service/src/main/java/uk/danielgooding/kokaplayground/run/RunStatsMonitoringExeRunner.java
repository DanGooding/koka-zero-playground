package uk.danielgooding.kokaplayground.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.danielgooding.kokaplayground.common.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class RunStatsMonitoringExeRunner {
    private static final String statsFileEnvVarName = "KOKA_WRITE_RUN_STATS";
    private static final Logger logger = LoggerFactory.getLogger(RunStatsMonitoringExeRunner.class);

    private final SandboxedExeRunner runner;
    private final Workdir workdir;
    private final ObjectMapper objectMapper;

    public RunStatsMonitoringExeRunner(SandboxedExeRunner runner, Workdir workdir) {
        this.runner = runner;
        this.workdir = workdir;
        objectMapper = new ObjectMapper();
    }

    public CancellableFuture<OrError<RunStats>> runStreamingStdinAndStdout(
            Path exe,
            List<String> args,
            Map<String, String> environment,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout
    ) {

        Map<String, String> extendedEnvironment = new HashMap<>(environment);
        List<Path> bindAdditionalReadWrite = new ArrayList<>();
        Path runStatsFile;
        try {
            runStatsFile = workdir.freshPath("run-stats");
            Files.createFile(runStatsFile);
            extendedEnvironment.put(statsFileEnvVarName, runStatsFile.toString());
            bindAdditionalReadWrite.add(runStatsFile);

        } catch (IOException e) {
            return CancellableFuture.failedFuture(e);
        }

        CancellableFuture<OrError<RunStats>> outcome =
                runner.runStreamingStdinAndStdout(
                                exe, args,
                                extendedEnvironment,
                                stdinBuffer,
                                onStart, onStdout,
                                List.of(), bindAdditionalReadWrite)
                        .thenApply(result -> {
                            switch (result) {
                                case Failed<Void> failed -> {
                                    return failed.castValue();
                                }
                                case Ok<Void> ok -> {
                                    try {
                                        RunStats runStats = RunStats.readFile(runStatsFile, objectMapper);
                                        return OrError.ok(runStats);

                                    } catch (IOException e) {
                                        logger.error("failed to get runStats", e);
                                        return OrError.ok(null);
                                    }
                                }
                            }
                        });

        return outcome.whenComplete((OrCancelled<OrError<RunStats>> result, Throwable exn) -> {
            try {
                Files.deleteIfExists(runStatsFile);
            } catch (IOException e) {
                logger.error("failed to delete {}", runStatsFile);
            }
        });
    }
}