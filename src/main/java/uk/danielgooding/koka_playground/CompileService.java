package uk.danielgooding.koka_playground;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CompileService {
    @Value("${compiler.exe-path}")
    private String compilerExePath;

    @Value("${compiler.koka-zero-config-path}")
    private String kokaZeroConfigPath;

    @Value("${compiler.workdir}")
    private String workdir;

    @Resource(name = "${which-exe-store}")
    private ExeStore<ExeHandle> exeStore;

    CompletableFuture<OrError<Void>> typecheck(KokaSourceCode sourceCode) {
        InputStream toStdin = new ByteArrayInputStream(sourceCode.getCode().getBytes(StandardCharsets.UTF_8));
        return Subprocess.run(compilerExePath, List.of("check", "/dev/stdin"), toStdin);
    }

    CompletableFuture<OrError<ExeHandle>> compile(KokaSourceCode sourceCode, boolean optimise) {

        try {
            Path workDir = Path.of(workdir);
            Files.createDirectories(workDir);

            Path runWorkdir = Files.createTempDirectory(Path.of(workdir), "compile");
            Path outputExePath = runWorkdir.resolve("main.exe");

            List<String> args = new ArrayList<>(List.of(
                    "compile",
                    "/dev/stdin",
                    "-config", kokaZeroConfigPath,
                    "-o", outputExePath.toString(),
                    "-save-temps-with", "output"
            ));

            if (optimise) {
                args.add("-optimise");
            }

            InputStream toStdin = new ByteArrayInputStream(sourceCode.getCode().getBytes(StandardCharsets.UTF_8));

            return Subprocess.run(compilerExePath, args, toStdin).thenCompose(
                    (result) -> {
                        switch (result) {
                            case Failed<Void> error -> {
                                return CompletableFuture.completedFuture(error.castValue());
                            }
                            case Ok<Void> _void -> {
                                try {
                                    return CompletableFuture.completedFuture(OrError.ok(exeStore.putExe(outputExePath)));
                                } catch (IOException e) {
                                    return CompletableFuture.failedFuture(e);
                                }
                            }

                        }
                    }
            );

        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
