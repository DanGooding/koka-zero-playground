package uk.danielgooding.koka_playground;

import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Subprocess {

    static CompletableFuture<OrError<Void>> run(String command, List<String> args, InputStream toStdin) {
        try {
            List<String> commandAndArgs = new ArrayList<>();
            commandAndArgs.add(command);
            commandAndArgs.addAll(args);

            ProcessBuilder processBuilder = new ProcessBuilder(commandAndArgs);
            Process process = processBuilder.start();
            OutputStream stdin = process.getOutputStream();
            StreamUtils.copy(toStdin, stdin);
            stdin.close();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return CompletableFuture.completedFuture(OrError.ok(null));
            }

            String error = new String(process.getErrorStream().readAllBytes());
            return CompletableFuture.completedFuture(OrError.error(error));

        } catch (IOException | InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }


}
