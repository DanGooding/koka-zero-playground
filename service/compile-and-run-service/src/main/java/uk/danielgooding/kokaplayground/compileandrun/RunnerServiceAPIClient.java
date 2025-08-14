package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.core.ParameterizedTypeReference;
import uk.danielgooding.kokaplayground.common.ExeHandle;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.CompletableFuture;

public class RunnerServiceAPIClient {
    private final APIClient apiClient;

    public RunnerServiceAPIClient(APIClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<String> run(ExeHandle handle) {
        ParameterizedTypeReference<OrError<String>> stdoutOrErrorType =
                new ParameterizedTypeReference<OrError<String>>() {
                };

        return apiClient
                .postExpectOk("/run", handle, stdoutOrErrorType);

    }

}
