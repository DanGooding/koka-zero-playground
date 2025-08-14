package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import uk.danielgooding.kokaplayground.common.Failed;
import uk.danielgooding.kokaplayground.common.Ok;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.CompletableFuture;

public class APIClient {
    private final RestClient restClient;

    public APIClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /// wraps an HTTP call to the RestClient into a future.
    /// the result is failure for any non 200 code,
    /// or if the response deserializes to null
    public <Req, Res> CompletableFuture<Res> method(
            HttpMethod method,
            String path,
            Req requestBody,
            ParameterizedTypeReference<Res> responseType) {

        ResponseEntity<Res> response = restClient
                .method(method)
                .uri(path)
                .body(requestBody)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(responseType);

        if (!response.getStatusCode().is2xxSuccessful()) {
            return CompletableFuture.failedFuture(
                    new RuntimeException(String.format(
                            "%s %s: failed %s", method, path, response)));
        }

        Res responseBody = response.getBody();
        if (responseBody == null) {
            return CompletableFuture.failedFuture(
                    new RuntimeException(String.format(
                            "%s %s: failed to deserialize result %s", method, path, response)));
        }

        return CompletableFuture.completedFuture(responseBody);
    }

    public <Req, Res> CompletableFuture<Res> post(
            String path,
            Req body,
            ParameterizedTypeReference<Res> responseType) {
        return method(HttpMethod.POST, path, body, responseType);
    }

    /// like `method()` but the returned Future fails if the result is not Ok.
    public <Req, Res> CompletableFuture<Res> methodExpectOk(
            HttpMethod method,
            String path,
            Req body,
            ParameterizedTypeReference<OrError<Res>> responseType) {
        return method(HttpMethod.POST, path, body, responseType)
                .thenCompose(maybeResponse ->
                        switch (maybeResponse) {
                            case Ok<Res> resOk -> CompletableFuture.completedFuture(resOk.getValue());
                            case Failed<?> failed -> CompletableFuture.failedFuture(new RuntimeException(String.format(
                                    "%s %s: returned error: %s", method, path, failed.getMessage())));
                        });
    }

    public <Req, Res> CompletableFuture<Res> postExpectOk(
            String path,
            Req body,
            ParameterizedTypeReference<OrError<Res>> responseType) {
        return methodExpectOk(HttpMethod.POST, path, body, responseType);
    }

}
