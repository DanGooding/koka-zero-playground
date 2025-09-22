package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

@Service
public class RunnerService {
    private final ExeStore exeStore;

    private final SandboxedExeRunner exeRunner;

    private final Workdir workdir;

    private final Duration realTimeLimit;

    public RunnerService(
            @Autowired
            ExeStore exeStore,

            @Autowired
            SandboxedExeRunner exeRunner,

            @Autowired
            Workdir.WebsocketServerSessionScoped workdir,

            @Value("${runner.real-time-limit-seconds}")
            int realTimeLimitSeconds) {
        this.exeStore = exeStore;
        this.exeRunner = exeRunner;
        this.workdir = workdir;
        this.realTimeLimit = Duration.ofSeconds(realTimeLimitSeconds);
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

            CancellableFuture<OrError<Void>> runOutcome =
                    exeRunner.runStreamingStdinAndStdout(exe, List.of(), stdinBuffer, onStart, onStdout, realTimeLimit);

            return runOutcome.whenComplete((ignored, exn) -> {
                try {
                    exeStore.deleteExe(handle);
                } catch (IOException e) {
                    // best effort deletion - we can have some pruning
                }
            });

        } catch (IOException e) {
            return CancellableFuture.failedFuture(e);
        }
    }
}
