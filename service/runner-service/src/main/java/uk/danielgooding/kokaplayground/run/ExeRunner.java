package uk.danielgooding.kokaplayground.run;

import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.Subprocess;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class ExeRunner implements IExeRunner {

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
        return Subprocess.runThenGetStdout(exe, args, stdin).thenApply(output -> {
            return resultForOutput(output, output::stdout);
        });
    }

    public CompletableFuture<OrError<Void>> runStreamingStdinAndStdout(
            Path exe,
            List<String> args,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout) {

        return Subprocess.runStreamingStdinAndStdout(exe, args, stdinBuffer, onStart, onStdout)
                .thenApply(output -> resultForOutput(output, () -> null));
    }
}
