package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.util.concurrent.CompletableFuture;

@Service
public class CompileAndRunService {

    private final CompileServiceAPIClient compileServiceAPIClient;
    private final CollectingRunnerWebSocketClient runnerWebSocketClient;


    public CompileAndRunService(
            @Autowired CompileServiceAPIClient compileServiceAPIClient,
            @Autowired CollectingRunnerWebSocketClient runnerWebSocketClient) {
        this.compileServiceAPIClient = compileServiceAPIClient;
        this.runnerWebSocketClient = runnerWebSocketClient;

    }

    public CompletableFuture<OrError<String>> compileAndRun(KokaSourceCode sourceCode) {

        return OrError.thenComposeFuture(
                compileServiceAPIClient.compile(sourceCode),
                handle ->
                        runnerWebSocketClient.execute().thenCompose(
                                (session) -> {

                                    try {
                                        session.sendMessage(new RunStream.Inbound.Run(handle));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                    return session.getOutcomeFuture();
                                })

        );
    }

}
