package uk.danielgooding.kokaplayground.common;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.socket.client.WebSocketClient;

import java.util.concurrent.CompletableFuture;

public class TypedWebSocketClient<InboundMessage, OutboundMessage, SessionState> {
    private final WebSocketClient webSocketClient;
    private final UntypedWrapperWebSocketHandler<InboundMessage, OutboundMessage, SessionState> handler;

    public TypedWebSocketClient(
            WebSocketClient webSocketClient,
            TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState> handler,
            Class<InboundMessage> inboundMessageClass,
            Jackson2ObjectMapperBuilder objectMapperBuilder,
            ConcurrentWebSocketWriteLimits writeLimits
    ) {
        this.webSocketClient = webSocketClient;
        this.handler = new UntypedWrapperWebSocketHandler<>(handler, inboundMessageClass, objectMapperBuilder, writeLimits);
    }

    public CompletableFuture<TypedWebSocketSessionAndState<OutboundMessage, SessionState>> execute(String uri) {
        return webSocketClient.execute(handler, uri)
                .thenApply(handler::getSessionAndState);
    }
}
