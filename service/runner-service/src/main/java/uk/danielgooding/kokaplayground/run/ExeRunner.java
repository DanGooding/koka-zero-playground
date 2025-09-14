package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.CancellableFuture;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.Subprocess;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Service
public class ExeRunner implements IExeRunner {
    private static final Logger logger = LoggerFactory.getLogger(ExeRunner.class);

    @Autowired
    @Qualifier("stdin-writer")
    Executor stdinWriterExecutor;

    @Autowired
    @Qualifier("stdout-reader")
    Executor stdoutReaderExecutor;

    private <T> OrError<T> resultForOutput(Subprocess.Output output, Supplier<T> getOkResult) {
        if (output.isExitSuccess()) {
            if (!output.stderr().isBlank()) {
                logger.warn("exe exited ok but with stderr: '{}'", output.stderr());
            }
            return OrError.ok(getOkResult.get());
        } else if (output.stderr().isBlank()) {
            return OrError.error(output.exitCode().errorMessage());
        } else {
            return OrError.error(output.stderr());
        }
    }

    public CompletableFuture<OrError<String>> runThenGetStdout(
            Path exe, List<String> args, Map<String, String> environment, String stdin) {
        return Subprocess.runThenGetStdout(exe, args, environment, stdin)
                .thenApply(output -> resultForOutput(output, output::stdout));
    }

    public CancellableFuture<OrError<Void>> runStreamingStdinAndStdout(
            Path exe,
            List<String> args,
            Map<String, String> environment,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout) {

        return Subprocess.runStreamingStdinAndStdout(
                        exe, args, environment, stdinBuffer, onStart, onStdout, stdoutReaderExecutor, stdinWriterExecutor)
                .thenApply(output -> resultForOutput(output, () -> null));
    }
}
