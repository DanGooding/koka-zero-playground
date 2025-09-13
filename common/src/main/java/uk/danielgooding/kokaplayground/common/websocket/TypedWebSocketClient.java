package uk.danielgooding.kokaplayground.common.websocket;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TypedWebSocketClient<
        InboundMessage,
        OutboundMessage,
        SessionState extends ISessionState<SessionStateTag>,
        SessionStateTag,
        Context,
        Outcome> {
    private final WebSocketClient webSocketClient;
    private final Function<
            Context,
            UntypedWrapperWebSocketHandler<InboundMessage, OutboundMessage, SessionState, SessionStateTag, Outcome>>
            handlerFactory;
    private final ConcurrentWebSocketWriteLimits writeLimits;

    public TypedWebSocketClient(
            WebSocketClient webSocketClient,
            Function<Context, TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, SessionStateTag, Outcome>>
                    handlerFactory,
            Class<InboundMessage> inboundMessageClass,
            Jackson2ObjectMapperBuilder objectMapperBuilder,
            ConcurrentWebSocketWriteLimits writeLimits,
            MeterRegistry meterRegistry
    ) {
        this.webSocketClient = webSocketClient;

        this.handlerFactory = (context) ->
                new UntypedWrapperWebSocketHandler<>(
                        handlerFactory.apply(context),
                        inboundMessageClass,
                        objectMapperBuilder,
                        meterRegistry);

        this.writeLimits = writeLimits;
    }

    private WebSocketHandler decorateHandler(WebSocketHandler handler) {
        return new LoggingWebSocketHandlerDecorator(handler);
    }

    public CompletableFuture<TypedWebSocketSession<OutboundMessage, Outcome>> execute(String uri, Context context) {
        UntypedWrapperWebSocketHandler<InboundMessage, OutboundMessage, SessionState, SessionStateTag, Outcome>
                handler = handlerFactory.apply(context);
        RealWebSocketHandler realHandler = new RealWebSocketHandler(handler, writeLimits);
        WebSocketHandler decoratedHandler = decorateHandler(realHandler);
        return webSocketClient.execute(decoratedHandler, uri)
                .thenApply(session -> handler.getSessionAndState(session).getSession());
    }
}
