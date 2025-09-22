package uk.danielgooding.kokaplayground.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class Subprocess {

    private static final Logger logger = LoggerFactory.getLogger(Subprocess.class);

    public record Output(boolean interrupted, ExitCode exitCode, String stdout, String stderr) {

        public boolean isInterrupted() {
            return interrupted;
        }

        public boolean isExitSuccess() {
            return !interrupted && exitCode.isSuccess();
        }
    }

    /// runs a command non-interactively with the given args
    /// does a blocking write with the contents of `toStdin`
    /// then a blocking read of stdout then stdin
    /// collecting these into Output
    public static CompletableFuture<Output> runThenGetStdout(Path command, List<String> args, String toStdin) {
        try {
            List<String> commandAndArgs = new ArrayList<>();
            commandAndArgs.add(command.toString());
            commandAndArgs.addAll(args);

            ProcessBuilder processBuilder = new ProcessBuilder(commandAndArgs);
            Process process = processBuilder.start();
            OutputStream stdin = process.getOutputStream();

            stdin.write(toStdin.getBytes(StandardCharsets.UTF_8));
            stdin.close();

            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            int exitCode = process.waitFor();
            logger.debug("process exited {} with code {}", command, exitCode);

            Output output = new Output(false, new ExitCode(exitCode), stdout, stderr);
            return CompletableFuture.completedFuture(output);

        } catch (IOException | InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /// Runs a command, providing it stdin and exposing its stdout in a streaming fashion.
    /// - onStart will be called once the process is spawned
    /// - elements will be pulled from toStdin and written to the process
    /// - onStdout will be called with chunks of stdout
    /// - then the future will resolve, and onStdout will not be called any further
    /// - the returned Output will have stdout=null
    ///
    /// This spawns two java threads alongside the subprocess - one to read its stdout,
    /// and one to write stdin to it. To avoid deadlocks in limited-pool executors, callers
    /// can provide two separate executors for these jobs to be run in.
    public static CancellableFuture<OrInterrupted<Output>> runStreamingStdinAndStdout(
            Path command,
            List<String> args,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout,
            Duration realTimeLimit,
            Executor runTimeLimiterExecutor,
            Executor stdoutReaderExecutor,
            Executor stdinWriterExecutor) {

        List<String> commandAndArgs = new ArrayList<>();
        commandAndArgs.add(command.toString());
        commandAndArgs.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(commandAndArgs);


        return CancellableFuture.supplyAsync(canceler -> {
            try {
                Process process = processBuilder.start();
                canceler.setOnCancel(process::destroyForcibly);
                onStart.call(null);


                // since write() can block, the point of this thread is to be blocked,
                // so that the thread running the websocket handler doesn't.
                CompletableFuture.runAsync(() -> {
                    try (OutputStream stdin = process.getOutputStream()) {

                        // we can't signal that we're done writing stdin (BlockingQueue has no close())
                        // so to avoid leaking this worker thread (it would block forever on take())
                        // use poll() with a timeout and check processCompleted
                        while (process.isAlive()) {
                            String maybeChunk = stdinBuffer.poll(1, TimeUnit.MILLISECONDS);
                            if (maybeChunk == null) continue;

                            stdin.write(maybeChunk.getBytes(StandardCharsets.UTF_8));
                            stdin.flush();
                        }

                    } catch (IOException | InterruptedException e) {
                        // this thread will always terminate as the write call will
                        // throw once the stream is closed
                        // this simply means the process didn't eat all of toStdin
                        canceler.cancel();
                    }
                }, stdinWriterExecutor);

                CompletableFuture.runAsync(() -> {
                    try {
                        InputStream stdout = process.getInputStream();

                        byte[] buf = new byte[1024];
                        int numRead;
                        while ((numRead = stdout.read(buf)) > 0) {
                            onStdout.call(new String(buf, 0, numRead, StandardCharsets.UTF_8));
                        }
                        // exits normally on EOF

                    } catch (IOException e) {
                        canceler.cancel();
                    }

                }, stdoutReaderExecutor);

                boolean completedInTimeLimit = process.waitFor(realTimeLimit.toMillis(), TimeUnit.MILLISECONDS);


                if (completedInTimeLimit) {
                    int exitCode = process.exitValue();
                    logger.debug("process {} exited with code {}", command, exitCode);

                    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                    Output output = new Output(false, new ExitCode(exitCode), null, stderr);

                    return OrCancelled.ok(OrInterrupted.ok(output));
                } else {
                    logger.debug("process {} timed out (limit {})", command, realTimeLimit);

                    return OrCancelled.ok(OrInterrupted.interrupted());
                }

            } catch (InterruptedException | IOException e) {
                canceler.cancel();
                throw new RuntimeException(e);
            } catch (CancelledException e) {
                return OrCancelled.cancelled();
            }
        }, runTimeLimiterExecutor);

    }

}
