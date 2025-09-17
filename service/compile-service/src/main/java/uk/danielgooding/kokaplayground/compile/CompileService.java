package uk.danielgooding.kokaplayground.compile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class CompileService {
    @Autowired
    private ExeCache exeCache;

    @Autowired
    private CompilerTool compilerTool;

    @Autowired
    private ExeStore exeStore;

    private static final Logger logger = LoggerFactory.getLogger(CompileService.class);

    public CompletableFuture<OrError<Void>> typecheck(KokaSourceCode sourceCode) {
        return compilerTool.typecheck(sourceCode);
    }

    public CompletableFuture<OrError<ExeHandle>> compile(KokaSourceCode sourceCode, boolean optimise) {

        Optional<byte[]> compiledExe = exeCache.getCompiledExe(sourceCode, optimise);

        if (compiledExe.isEmpty()) {
            logger.info("exe not found in cache, compiling");

            CompletableFuture<OrError<Path>> result = compilerTool.compile(sourceCode, optimise);

            return result.thenCompose(
                    (maybeLocalExe) -> {
                        switch (maybeLocalExe) {
                            case Failed<?> error -> {
                                // we don't cache errors
                                return CompletableFuture.completedFuture(error.castValue());
                            }
                            case Ok<Path> localExe -> {
                                try {
                                    byte[] exe = Files.readAllBytes(localExe.getValue());
                                    exeCache.putCompiledExe(sourceCode, optimise, exe);

                                    return uploadAndReturnExe(exe);
                                } catch (IOException e) {
                                    return CompletableFuture.failedFuture(e);
                                }
                            }

                        }
                    }
            );

        } else {
            logger.info("exe found in cache, skipping compiler");
            return uploadAndReturnExe(compiledExe.get());
        }
    }

    public CompletableFuture<OrError<ExeHandle>> uploadAndReturnExe(byte[] exe) {
        try {
            ExeHandle handle = exeStore.putExe(exe);
            return CompletableFuture.completedFuture(OrError.ok(handle));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
