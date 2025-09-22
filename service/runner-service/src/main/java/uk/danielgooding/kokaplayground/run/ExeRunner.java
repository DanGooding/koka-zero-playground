package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Service
public class ExeRunner implements IExeRunner {
    @Autowired
    @Qualifier("stdin-writer")
    Executor stdinWriterExecutor;

    @Autowired
    @Qualifier("stdout-reader")
    Executor stdoutReaderExecutor;

    @Autowired
    @Qualifier("run-time-limiter")
    Executor runTimeLimiterExecutor;

    private <T> OrError<T> resultForOutput(Subprocess.Output output, Supplier<T> getOkResult) {
        if (output.isExitSuccess()) {
            return OrError.ok(getOkResult.get());
        } else if (output.stderr().isBlank()) {
            return OrError.error(output.exitCode().errorMessage());
        } else {
            return OrError.error(output.stderr());
        }
    }

    public CompletableFuture<OrError<String>> runThenGetStdout(Path exe, List<String> args, String stdin) {
        return Subprocess.runThenGetStdout(exe, args, stdin).thenApply(output -> resultForOutput(output, output::stdout));
    }

    public CancellableFuture<OrInterrupted<OrError<Void>>> runStreamingStdinAndStdout(
            Path exe,
            List<String> args,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout,
            Duration realTimeLimit) {

        return Subprocess.runStreamingStdinAndStdout(
                        exe, args, stdinBuffer, onStart, onStdout, realTimeLimit,
                        runTimeLimiterExecutor, stdoutReaderExecutor, stdinWriterExecutor)
                .thenApply(result -> result.map(output -> resultForOutput(output, () -> null)));
    }
}
