package uk.danielgooding.koka_playground;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CompilerTool {
    @Value("${compiler.exe-path}")
    private String compilerExePath;

    @Value("${compiler.koka-zero-config-path}")
    private String kokaZeroConfigPath;

    @Autowired
    private Workdir workdir;

    CompletableFuture<OrError<Void>> typecheck(KokaSourceCode sourceCode) {
        InputStream toStdin = new ByteArrayInputStream(sourceCode.getCode().getBytes(StandardCharsets.UTF_8));
        return Subprocess.runNoStdout(
                Path.of(compilerExePath),
                List.of("check", "/dev/stdin"), toStdin);
    }

    CompletableFuture<OrError<LocalExeHandle>> compile(KokaSourceCode sourceCode, boolean optimise) {


        LocalExeHandle outputExe;
        try {
            outputExe = new LocalExeHandle(workdir.freshPath("compiled"));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        List<String> args = new ArrayList<>(List.of(
                "compile",
                "/dev/stdin",
                "-config", kokaZeroConfigPath,
                "-o", outputExe.getPath().toString(),
                "-save-temps-with", "output"
        ));

        if (optimise) {
            args.add("-optimise");
        }

        InputStream toStdin = new ByteArrayInputStream(sourceCode.getCode().getBytes(StandardCharsets.UTF_8));

        return Subprocess.runNoStdout(
                        Path.of(compilerExePath),
                        args, toStdin)
                .thenApply(
                        (result) -> {
                            switch (result) {
                                case Failed<Void> error -> {
                                    return error.castValue();
                                }
                                case Ok<Void> _void -> {
                                    return OrError.ok(outputExe);
                                }
                            }
                        }
                );
    }
}
