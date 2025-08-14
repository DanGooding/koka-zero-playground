package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;

import java.util.concurrent.CompletableFuture;

@Service
public class CompileAndRunService {

    @Autowired
    CompileServiceAPIClient compileServiceAPIClient;

    @Autowired
    RunnerServiceAPIClient runnerServiceAPIClient;

    public CompletableFuture<OrError<String>> compileAndRun(KokaSourceCode sourceCode) {

        return OrError.thenComposeFuture(
                compileServiceAPIClient.compile(sourceCode),
                handle ->
                        runnerServiceAPIClient.run(handle)
                                .thenApply(OrError::ok));
    }

}
