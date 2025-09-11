package uk.danielgooding.kokaplayground.compile;

import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CompilerTool {
    @Value("${compiler.exe-path}")
    private Path compilerExePath;

    @Value("${compiler.koka-zero-config-path}")
    private Path kokaZeroConfigPath;

    @Autowired
    private Workdir.RequestScoped workdir;

    public CompletableFuture<OrError<Void>> runCompiler(List<String> args, String toStdin) {
        return Subprocess.runThenGetStdout(compilerExePath, args, toStdin).thenCompose(output ->
                switch (output.exitCode()) {
                    // success
                    case 0 -> CompletableFuture.completedFuture(OrError.ok(null));
                    // error in user's code
                    case 1 -> CompletableFuture.completedFuture(OrError.error(output.stderr()));
                    // error in compiler usage
                    default -> CompletableFuture.failedFuture(new RuntimeException(output.stderr()));
                });
    }

    public CompletableFuture<OrError<Void>> typecheck(KokaSourceCode sourceCode) {
        return runCompiler(List.of("check", "/dev/stdin"), sourceCode.getCode());
    }

    @Timed(value = "compile.tool.compile")
    public CompletableFuture<OrError<Path>> compile(KokaSourceCode sourceCode, boolean optimise) {


        Path outputExe;
        try {
            outputExe = workdir.freshPath("compiled");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        List<String> args = new ArrayList<>(List.of(
                "compile",
                "/dev/stdin",
                "-config", kokaZeroConfigPath.toString(),
                "-o", outputExe.toString(),
                "-save-temps-with", "output"
        ));

        if (optimise) {
            args.add("-optimise");
        }

        return runCompiler(args, sourceCode.getCode())
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
