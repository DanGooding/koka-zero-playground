package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ConcurrentWebSocketWriteLimits;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketClient;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.RunStreamInbound;
import uk.danielgooding.kokaplayground.protocol.RunStreamOutbound;

import java.net.URI;
import java.util.concurrent.CompletableFuture;


@Service
public class RunnerWebSocketClient {
    private final String uri;
    private final TypedWebSocketClient<RunStreamOutbound.Message, RunStreamInbound.Message, RunnerClientWebSocketState, OrError<String>> client;

    public RunnerWebSocketClient(
            @Autowired WebSocketClient webSocketClient,
            @Value("${runner-service-hostname}") URI host,
            @Autowired RunnerClientWebSocketHandler handler,
            @Autowired Jackson2ObjectMapperBuilder objectMapperBuilder,
            @Autowired ConcurrentWebSocketWriteLimits writeLimits) {
        this.client = new TypedWebSocketClient<>(
                webSocketClient, handler, RunStreamOutbound.Message.class, objectMapperBuilder, writeLimits);
        this.uri = String.format("ws://%s/ws/run", host);
    }

    public CompletableFuture<TypedWebSocketSessionAndState<RunStreamInbound.Message, RunnerClientWebSocketState, OrError<String>>> execute() {
        return client.execute(uri);
    }
}
