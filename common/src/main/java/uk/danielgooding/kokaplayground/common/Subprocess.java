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

    public static class Output {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public Output(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isExitSuccess() {
            return exitCode == 0;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }
    }

    public static CompletableFuture<Output> run(Path command, List<String> args, InputStream toStdin) {
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
            String stderr = new String(process.getErrorStream().readAllBytes());

            int exitCode = process.waitFor();
            return CompletableFuture.completedFuture(new Output(exitCode, stdout, stderr));

        } catch (IOException | InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

}
