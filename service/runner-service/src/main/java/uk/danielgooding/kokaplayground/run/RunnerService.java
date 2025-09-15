package uk.danielgooding.kokaplayground.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Service
public class RunnerService {
    private static final Logger logger = LoggerFactory.getLogger(RunnerService.class);

    private final ExeStore exeStore;
    private final RunStatsMonitoringExeRunner exeRunner;
    private final Workdir workdir;

    public RunnerService(
            @Autowired ExeStore exeStore,
            @Autowired SandboxedExeRunner exeRunner,
            @Autowired Workdir.WebsocketServerSessionScoped workdir) {
        this.exeStore = exeStore;
        this.exeRunner = new RunStatsMonitoringExeRunner(exeRunner, workdir);
        this.workdir = workdir;
    }

    public CancellableFuture<OrError<Void>> runStreamingStdinAndStdout(
            ExeHandle handle, BlockingQueue<String> stdinBuffer, Callback<Void> onStart, Callback<String> onStdout) {
        try {
            Path exe;
            switch (exeStore.getExe(handle, workdir)) {
                case Failed<?> failed -> {
                    return CancellableFuture.completedFuture(failed.castValue());
                }
                case Ok<Path> okExe -> {
                    exe = okExe.getValue();
                }
            }

            return exeRunner
                    .runStreamingStdinAndStdout(exe, List.of(), Map.of(), stdinBuffer, onStart, onStdout)
                    .whenComplete((maybeRunStats, exn) -> {
                        // TODO: stop logging - report a metric instead
                        logger.error("got runStats {}", maybeRunStats);
                        try {
                            exeStore.deleteExe(handle);
                        } catch (IOException e) {
                            // best effort deletion - we can have some pruning
                        }
                    })
                    .thenApply((maybeRunStats) -> switch (maybeRunStats) {
                        case Failed<?> failed -> failed.castValue();
                        case Ok<RunStats> ok -> OrError.ok(null);
                    });

        } catch (IOException e) {
            return CancellableFuture.failedFuture(e);
        }
    }
}
