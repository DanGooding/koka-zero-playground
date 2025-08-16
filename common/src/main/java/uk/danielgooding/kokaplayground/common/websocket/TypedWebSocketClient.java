package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

import java.util.concurrent.CompletableFuture;

public class TypedWebSocketClient<InboundMessage, OutboundMessage, SessionState> {
    private final WebSocketClient webSocketClient;
    private final UntypedWrapperWebSocketHandler<InboundMessage, OutboundMessage, SessionState> handler;
    private final WebSocketHandler decoratedHandler;

    public TypedWebSocketClient(
            WebSocketClient webSocketClient,
            TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState> handler,
            Class<InboundMessage> inboundMessageClass,
            Jackson2ObjectMapperBuilder objectMapperBuilder,
            ConcurrentWebSocketWriteLimits writeLimits
    ) {
        this.webSocketClient = webSocketClient;
        this.handler = new UntypedWrapperWebSocketHandler<>(handler, inboundMessageClass, objectMapperBuilder, writeLimits);
        this.decoratedHandler = new LoggingWebSocketHandlerDecorator(this.handler);
    }

    public CompletableFuture<TypedWebSocketSessionAndState<OutboundMessage, SessionState>> execute(String uri) {
        return webSocketClient.execute(decoratedHandler, uri)
                .thenApply(handler::getSessionAndState);
    }
}
