package uk.danielgooding.kokaplayground.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Hashtable;

// TODO: bytes if we're just using json?
public class UntypedWrapperWebsocketHandler<InboundMessage, OutboundMessage> extends TextWebSocketHandler {

    private final Hashtable<String, TypedWebsocketSession<OutboundMessage>> runnerSessions;
    private final ObjectMapper objectMapper;

    private final TypedWebSocketHandler<InboundMessage, OutboundMessage> typedWebSocketHandler;

    private final Class<InboundMessage> inboundMessageClass;

    private final ConcurrentWebSocketWriteLimits writeLimits;

    public UntypedWrapperWebsocketHandler(
            TypedWebSocketHandler<InboundMessage, OutboundMessage> typedWebSocketHandler,
            Class<InboundMessage> inboundMessageClass,
            Jackson2ObjectMapperBuilder objectMapperBuilder,
            ConcurrentWebSocketWriteLimits writeLimits
    ) {
        objectMapper = objectMapperBuilder.build();
        runnerSessions = new Hashtable<>();
        this.typedWebSocketHandler = typedWebSocketHandler;
        this.inboundMessageClass = inboundMessageClass;
        this.writeLimits = writeLimits;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        TypedWebsocketSession<OutboundMessage> runnerSession = new TypedWebsocketSession<>(
                session, objectMapper, writeLimits);
        runnerSessions.put(session.getId(), runnerSession);
        typedWebSocketHandler.handleConnectionEstablished(runnerSession);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage textMessage) throws Exception {
        TypedWebsocketSession<OutboundMessage> runnerSession = runnerSessions.get(session.getId());
        InboundMessage message =
                objectMapper.readValue(textMessage.asBytes(), this.inboundMessageClass);
        typedWebSocketHandler.handleMessage(runnerSession, message);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        TypedWebsocketSession<OutboundMessage> runnerSession = runnerSessions.remove(session.getId());
        typedWebSocketHandler.afterConnectionClosed(runnerSession, status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        TypedWebsocketSession<OutboundMessage> runnerSession = runnerSessions.get(session.getId());
        typedWebSocketHandler.handleTransportError(runnerSession, exception);
    }
}
