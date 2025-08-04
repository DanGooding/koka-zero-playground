package uk.danielgooding.koka_playground;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CompileService {
    @Value("${compiler.exe-path}")
    private String exe_path;

    @Value("${compiler.koka-zero-config-path}")
    private String koka_zero_config_path;

    @Value("${compiler.workdir}")
    private String workdir;

    CompletableFuture<OrError<Void>> typecheck(KokaSourceCode sourceCode) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(exe_path, "check", "/dev/stdin");
            Process process = processBuilder.start();

            OutputStream stdin = process.getOutputStream();
            stdin.write(sourceCode.getCode().getBytes(StandardCharsets.UTF_8));
            stdin.close();

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return CompletableFuture.completedFuture(OrError.ok(null));
            }

            String error = new String(process.getErrorStream().readAllBytes());

            return CompletableFuture.completedFuture(OrError.error(error));

        }catch (IOException | InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    CompletableFuture<OrError<ExecutableHandle>> compile(KokaSourceCode sourceCode, boolean optimise) {

        try {
            Path workDir = Path.of(workdir);
            Files.createDirectories(workDir);

            Path runWorkdir = Files.createTempDirectory(Path.of(workdir), "compile");
            Path outputExePath = runWorkdir.resolve("main.exe");


            List<String> args = new ArrayList<>(List.of(
                    exe_path, "compile",
                    "/dev/stdin",
                    "-config", koka_zero_config_path,
                    "-o", outputExePath.toString(),
                    "-save-temps-with", "output"
                    ));

            if (optimise) {
                args.add("-optimise");
            }

            ProcessBuilder processBuilder = new ProcessBuilder(args);
            Process process = processBuilder.start();

            OutputStream stdin = process.getOutputStream();
            stdin.write(sourceCode.getCode().getBytes(StandardCharsets.UTF_8));
            stdin.close();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String error = new String(process.getErrorStream().readAllBytes());
                return CompletableFuture.completedFuture(OrError.error(error));
            }

            ExecutableHandle executableHandle = new ExecutableHandle(outputExePath);
            return CompletableFuture.completedFuture(OrError.ok(executableHandle));

        }catch (IOException | InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
