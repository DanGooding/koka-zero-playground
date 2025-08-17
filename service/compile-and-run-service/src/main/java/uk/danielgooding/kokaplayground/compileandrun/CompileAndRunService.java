package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.protocol.RunStreamInbound;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;

@Service
public class CompileAndRunService {

    private final CompileServiceAPIClient compileServiceAPIClient;
    private final RunnerWebSocketClient runnerWebSocketClient;


    public CompileAndRunService(
            @Autowired CompileServiceAPIClient compileServiceAPIClient,
            @Autowired RunnerWebSocketClient runnerWebSocketClient) {
        this.compileServiceAPIClient = compileServiceAPIClient;
        this.runnerWebSocketClient = runnerWebSocketClient;

    }

    public CompletableFuture<OrError<String>> compileAndRun(KokaSourceCode sourceCode) {

        return OrError.thenComposeFuture(
                compileServiceAPIClient.compile(sourceCode),
                handle ->
                        runnerWebSocketClient.execute().thenCompose(
                                sessionAndState -> {

                                    try {
                                        sessionAndState
                                                .getSession()
                                                .sendMessage(new RunStreamInbound.Run(handle));
                                    } catch (IOException e) {
                                        // TODO: do i need to do anything to ensure the connection is closed?
                                        throw new UncheckedIOException(e);
                                    }

                                    return sessionAndState.getOutcomeFuture();
                                })

        );
    }

}
