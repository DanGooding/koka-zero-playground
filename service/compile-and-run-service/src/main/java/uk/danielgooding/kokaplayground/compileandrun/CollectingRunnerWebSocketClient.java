package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ConcurrentWebSocketWriteLimits;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketClient;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


@Service
public class CollectingRunnerWebSocketClient {
    private final String uri;
    private final TypedWebSocketClient<
            RunStream.Outbound.Message,
            RunStream.Inbound.Message,
            CollectingRunnerClientWebSocketState,
            Void,
            OrError<String>> client;

    public CollectingRunnerWebSocketClient(
            @Autowired WebSocketClient webSocketClient,
            @Value("${runner-service-hostname}") URI host,
            @Autowired CollectingRunnerClientWebSocketHandler handler,
            @Autowired Jackson2ObjectMapperBuilder objectMapperBuilder,
            @Autowired ConcurrentWebSocketWriteLimits writeLimits) {

        Function<Void,
                TypedWebSocketHandler<RunStream.Outbound.Message,
                        RunStream.Inbound.Message,
                        CollectingRunnerClientWebSocketState,
                        OrError<String>>>
                handlerFactory = (ignored) -> handler;
        this.client = new TypedWebSocketClient<>(
                webSocketClient, handlerFactory, RunStream.Outbound.Message.class, objectMapperBuilder, writeLimits);
        this.uri = String.format("ws://%s/ws/run", host);
    }

    public CompletableFuture<TypedWebSocketSessionAndState<RunStream.Inbound.Message, CollectingRunnerClientWebSocketState, OrError<String>>> execute() {
        return client.execute(uri, null);
    }
}
