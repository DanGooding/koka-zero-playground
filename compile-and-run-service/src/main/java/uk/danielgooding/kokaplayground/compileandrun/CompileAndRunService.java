package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.ExeHandle;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.CompletableFuture;

@Service
public class CompileAndRunService {

    public CompletableFuture<OrError<ExeHandle>> compileAndRun(KokaSourceCode sourceCode) {
        throw new RuntimeException("compileAndRun: TODO: call compile and runner services");
    }

}
