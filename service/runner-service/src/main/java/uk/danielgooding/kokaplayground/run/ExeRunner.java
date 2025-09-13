package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.CancellableFuture;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.Subprocess;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Service
public class ExeRunner implements IExeRunner {
    // TODO: note we have to be careful - there is a potential deadlock when the thread pool is bounded
    //  and no task has started both the threads it needs
    //  - the solution is to have two executors for the two different threads needed
    @Autowired
    Executor executor;

    private <T> OrError<T> resultForOutput(Subprocess.Output output, Supplier<T> getOkResult) {
        if (output.isExitSuccess()) {
            return OrError.ok(getOkResult.get());
        } else if (output.stderr().isBlank()) {
            return OrError.error(String.format("exit %d", output.exitCode()));
        } else {
            return OrError.error(output.stderr());
        }
    }

    public CompletableFuture<OrError<String>> runThenGetStdout(Path exe, List<String> args, String stdin) {
        return Subprocess.runThenGetStdout(exe, args, stdin).thenApply(output -> resultForOutput(output, output::stdout));
    }

    public CancellableFuture<OrError<Void>> runStreamingStdinAndStdout(
            Path exe,
            List<String> args,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout) {

        return Subprocess.runStreamingStdinAndStdout(exe, args, stdinBuffer, onStart, onStdout, executor)
                .thenApply(output -> resultForOutput(output, () -> null));
    }
}
