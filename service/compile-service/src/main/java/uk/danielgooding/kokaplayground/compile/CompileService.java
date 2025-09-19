package uk.danielgooding.kokaplayground.compile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class CompileService {
    @Autowired
    private ExeCache exeCache;

    @Autowired
    private CachedCompilerTool cachedCompilerTool;

    @Autowired
    private CompilerArgs compilerArgs;

    @Autowired
    private ExeStore exeStore;

    private static final Logger logger = LoggerFactory.getLogger(CompileService.class);

    public CompletableFuture<OrError<Void>> typecheck(KokaSourceCode sourceCode) {
        return cachedCompilerTool.typecheck(sourceCode);
    }

    public CompletableFuture<OrError<ExeHandle>> compile(KokaSourceCode sourceCode) {

        return cachedCompilerTool.compile(sourceCode, compilerArgs)
                .thenCompose(maybeExe -> {
                    switch (maybeExe) {
                        case Failed<?> failed -> {
                            return CompletableFuture.completedFuture(failed.castValue());
                        }
                        case Ok<byte[]> okExe -> {
                            try {
                                ExeHandle handle = exeStore.putExe(okExe.getValue());
                                return CompletableFuture.completedFuture(OrError.ok(handle));
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(e);
                            }

                        }
                    }
                });
    }

}
