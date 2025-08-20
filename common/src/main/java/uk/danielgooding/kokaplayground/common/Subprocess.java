package uk.danielgooding.kokaplayground.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Subprocess {

    private static final Logger logger = LoggerFactory.getLogger(Subprocess.class);

    public record Output(int exitCode, String stdout, String stderr) {

        public boolean isExitSuccess() {
            return exitCode == 0;
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
            return CompletableFuture.completedFuture(new Output(exitCode, stdout, stderr));

        } catch (IOException | InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /// runs a command, providing it stdin and exposing its stdout in a streaming fashion
    /// onStart will be called once the process is spawned
    /// elements will be pulled from toStdin and written to the process
    /// onStdout will be called with chunks of stdout
    /// then the future will resolve, and onStdout will not be called any further
    /// Output will have stdout=null
    public static CompletableFuture<Output> runStreamingStdinAndStdout(
            Path command,
            List<String> args,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout) {

        List<String> commandAndArgs = new ArrayList<>();
        commandAndArgs.add(command.toString());
        commandAndArgs.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(commandAndArgs);

        return CompletableFuture.supplyAsync(() -> {

            try {
                Process process = processBuilder.start();
                onStart.call(null);

                AtomicBoolean processCompleted = new AtomicBoolean(false);

                // the process might not read all the provided stdin.
                // avoid causing a deadlock by blocking this thread
                // which would otherwise be waiting for the thread to exit.
                CompletableFuture.runAsync(() -> {
                    try (OutputStream stdin = process.getOutputStream()) {

                        // we can't signal that we're done writing stdin (BlockingQueue has no close())
                        // so to avoid leaking this worker thread (it would block forever on take())
                        // use poll() with a timeout and check processCompleted
                        while (!processCompleted.get()) {
                            String maybeChunk = stdinBuffer.poll(1, TimeUnit.MILLISECONDS);
                            if (maybeChunk == null) continue;

                            stdin.write(maybeChunk.getBytes(StandardCharsets.UTF_8));
                            stdin.flush();
                        }

                    } catch (IOException | InterruptedException e) {
                        // this thread will always terminate as the write call will
                        // throw once the stream is closed
                        // this simply means the process didn't eat all of toStdin
                    }
                });

                InputStream stdout = process.getInputStream();

                byte[] buf = new byte[1024];
                int numRead;
                while ((numRead = stdout.read(buf)) > 0) {
                    onStdout.call(new String(buf, 0, numRead, StandardCharsets.UTF_8));
                }

                int exitCode = process.waitFor();
                processCompleted.set(true);
                String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

                return new Output(exitCode, null, stderr);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

}
