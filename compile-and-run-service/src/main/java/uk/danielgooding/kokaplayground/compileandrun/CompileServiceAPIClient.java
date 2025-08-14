package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.core.ParameterizedTypeReference;
import uk.danielgooding.kokaplayground.common.ExeHandle;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.CompletableFuture;

public class CompileServiceAPIClient {
    private final APIClient apiClient;

    public CompileServiceAPIClient(APIClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<OrError<Void>> typecheck(KokaSourceCode sourceCode) {
        ParameterizedTypeReference<OrError<Void>> responseType =
                new ParameterizedTypeReference<OrError<Void>>() {
                };

        return apiClient.post("/typecheck", sourceCode, responseType);
    }

    public CompletableFuture<OrError<ExeHandle>> compile(KokaSourceCode sourceCode) {
        ParameterizedTypeReference<OrError<ExeHandle>> exeHandleOrErrorType =
                new ParameterizedTypeReference<OrError<ExeHandle>>() {
                };

        return apiClient.post("/compile", sourceCode, exeHandleOrErrorType);
    }
}
