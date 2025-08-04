package uk.danielgooding.koka_playground;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class CompileService {
    @Autowired
    private CompilerTool compilerTool;

    @Resource(name = "${which-exe-store}")
    private ExeStore exeStore;

    CompletableFuture<OrError<Void>> typecheck(KokaSourceCode sourceCode) {
        return compilerTool.typecheck(sourceCode);
    }

    CompletableFuture<OrError<ExeHandle>> compile(KokaSourceCode sourceCode, boolean optimise) {
        CompletableFuture<OrError<LocalExeHandle>> result = compilerTool.compile(sourceCode, optimise);

        return result.thenCompose(
                (maybeLocalExe) -> {
                    switch (maybeLocalExe) {
                        case Failed<?> error -> {
                            return CompletableFuture.completedFuture(error.castValue());
                        }
                        case Ok<LocalExeHandle> localExe -> {
                            try {
                                LocalExeHandle localExeHandle = new LocalExeHandle((localExe.getValue().getPath()));
                                ExeHandle handle = exeStore.putExe(localExeHandle);
                                return CompletableFuture.completedFuture(OrError.ok(handle));
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(e);
                            }
                        }

                    }
                }
        );

    }
}
