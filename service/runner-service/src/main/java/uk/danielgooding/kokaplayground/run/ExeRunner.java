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

@Service
public class ExeRunner implements IExeRunner {

    public CompletableFuture<OrError<String>> runThenGetStdout(Path exe, List<String> args, String stdin) {
        return Subprocess.runThenGetStdout(exe, args, stdin).thenApply(output -> {
            if (output.isExitSuccess()) {
                return OrError.ok(output.stdout());
            } else {
                return OrError.error(output.stderr());
            }
        });
    }

    public CompletableFuture<OrError<Void>> runStreamingStdinAndStdout(
            Path exe,
            List<String> args,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout) {

        return Subprocess.runStreamingStdinAndStdout(exe, args, stdinBuffer, onStart, onStdout)
                .thenApply(output -> {
                    if (output.isExitSuccess()) {
                        return OrError.ok(null);
                    } else {
                        return OrError.error(output.stderr());
                    }
                });
    }
}
