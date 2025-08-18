package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import uk.danielgooding.kokaplayground.common.websocket.ConcurrentWebSocketWriteLimits;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketClient;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


@Service
@Scope("websocket")
public class ProxyingRunnerWebSocketClient {
    private final String uri;
    private final TypedWebSocketClient<
            RunStream.Outbound.Message,
            RunStream.Inbound.Message,
            Void,
            ProxyingRunnerClientState,
            Void> client;

    public ProxyingRunnerWebSocketClient(
            @Autowired WebSocketClient webSocketClient,
            @Value("${runner-service-hostname}") URI host,
            @Autowired ProxyingRunnerClientWebSocketHandlerFactory handlerFactory,
            @Autowired Jackson2ObjectMapperBuilder objectMapperBuilder,
            @Autowired ConcurrentWebSocketWriteLimits writeLimits) {

        Function<
                ProxyingRunnerClientState,
                TypedWebSocketHandler<
                        RunStream.Outbound.Message,
                        RunStream.Inbound.Message,
                        Void,
                        Void>
                > handlerBuilder =
                (ProxyingRunnerClientState downstreamSessionAndState) -> {
                    handlerFactory.setDownstreamSessionAndState(downstreamSessionAndState);
                    return handlerFactory.getObject();
                };

        this.client = new TypedWebSocketClient<>(
                webSocketClient, handlerBuilder, RunStream.Outbound.Message.class, objectMapperBuilder, writeLimits);
        // TODO: probably share this part with the other client
        this.uri = String.format("ws://%s/ws/run", host);
    }

    public CompletableFuture<TypedWebSocketSessionAndState<RunStream.Inbound.Message, Void, Void>>
    execute(ProxyingRunnerClientState downstreamSessionAndState) {
        return client.execute(uri, downstreamSessionAndState);
    }
}
