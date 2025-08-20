package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

@Service
public class RunnerService {
    private ExeStore exeStore;

    private SandboxedExeRunner exeRunner;

    @Qualifier("runner-workdir")
    private Workdir workdir;

    public RunnerService(
            @Autowired
            ExeStore exeStore,

            @Autowired
            SandboxedExeRunner exeRunner,

            @Autowired
            @Qualifier("runner-workdir")
            Workdir workdir) {
        this.exeStore = exeStore;
        this.exeRunner = exeRunner;
        this.workdir = workdir;
    }

    public CompletableFuture<OrError<String>> runWithoutStdin(ExeHandle handle) {
        try {
            Path exe;
            switch (exeStore.getExe(handle, workdir)) {
                case Failed<?> failed -> {
                    return CompletableFuture.completedFuture(failed.castValue());
                }
                case Ok<Path> okExe -> {
                    exe = okExe.getValue();
                }
            }

            CompletableFuture<OrError<String>> stdout =
                    exeRunner.runThenGetStdout(exe, List.of(), "");

            exeStore.deleteExe(handle);

            return stdout;

        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<OrError<Void>> runStreamingStdinAndStdout(
            ExeHandle handle, BlockingQueue<String> stdinBuffer, Callback<Void> onStart, Callback<String> onStdout) {
        try {
            Path exe;
            switch (exeStore.getExe(handle, workdir)) {
                case Failed<?> failed -> {
                    return CompletableFuture.completedFuture(failed.castValue());
                }
                case Ok<Path> okExe -> {
                    exe = okExe.getValue();
                }
            }

            return exeRunner
                    .runStreamingStdinAndStdout(exe, List.of(), stdinBuffer, onStart, onStdout)
                    .whenComplete((ignored, exn) -> {
                        try {
                            exeStore.deleteExe(handle);
                        } catch (IOException e) {
                            // best effort deletion - we can have some pruning
                        }
                    });

        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
