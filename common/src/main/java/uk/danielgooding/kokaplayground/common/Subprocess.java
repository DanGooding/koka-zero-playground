package uk.danielgooding.kokaplayground.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class Subprocess {

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

    /// runs a command, exposing its stdout in a streaming fashion
    /// onStart will be called once the process is spawned
    /// onStdout will be called with chunks of stdout
    /// then the future will resolve, and onStdout will not be called any further
    /// Output will have stdout=null
    public static CompletableFuture<Output> runStreamingStdout(
            Path command,
            List<String> args,
            // TODO: stream stdin too
            String toStdin,
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

                // the process might not read all the provided stdin.
                // avoid causing a deadlock by blocking this thread
                // which would otherwise be waiting for the thread to exit.
                CompletableFuture.runAsync(() -> {
                    try {
                        OutputStream stdin = process.getOutputStream();
                        stdin.write(toStdin.getBytes(StandardCharsets.UTF_8));
                        stdin.close();

                    } catch (IOException e) {
                        // this thread will always terminate as the write call will
                        // throw once the stream is closed
                        // this simply means the process didn't eat all of toStdin
                    }
                });

                InputStream stdout = process.getInputStream();

                byte[] buf = new byte[256];
                int numRead;
                while ((numRead = stdout.read(buf)) > 0) {
                    onStdout.call(new String(buf, StandardCharsets.UTF_8));
                }

                int exitCode = process.waitFor();
                String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

                return new Output(exitCode, null, stderr);

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

    }

}
