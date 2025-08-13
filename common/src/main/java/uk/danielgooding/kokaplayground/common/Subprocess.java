package uk.danielgooding.kokaplayground.common;

import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Subprocess {

    public static CompletableFuture<OrError<String>> run(Path command, List<String> args, InputStream toStdin) {
        try {
            List<String> commandAndArgs = new ArrayList<>();
            commandAndArgs.add(command.toString());
            commandAndArgs.addAll(args);

            ProcessBuilder processBuilder = new ProcessBuilder(commandAndArgs);
            Process process = processBuilder.start();
            OutputStream stdin = process.getOutputStream();

            StreamUtils.copy(toStdin, stdin);
            stdin.close();

            String stdout = new String(process.getInputStream().readAllBytes());

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return CompletableFuture.completedFuture(OrError.ok(stdout));
            }

            String error = new String(process.getErrorStream().readAllBytes());
            return CompletableFuture.completedFuture(OrError.error(error));

        } catch (IOException | InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public static CompletableFuture<OrError<Void>> runNoStdout(Path command, List<String> args, InputStream toStdin) {
        return run(command, args, toStdin).thenApply((maybeStdout) ->
                switch (maybeStdout) {
                    case Ok<String> _stdout -> Ok.ok(null);
                    case Failed<String> error -> error.castValue();
                });

    }

}
