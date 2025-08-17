package uk.danielgooding.kokaplayground.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.Hashtable;

public class UntypedWrapperWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> extends BinaryWebSocketHandler {

    private final Hashtable<String, TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome>> typedSessions;
    private final ObjectMapper objectMapper;

    private final TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> typedWebSocketHandler;

    private final Class<InboundMessage> inboundMessageClass;

    private final ConcurrentWebSocketWriteLimits writeLimits;

    public UntypedWrapperWebSocketHandler(
            TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> typedWebSocketHandler,
            Class<InboundMessage> inboundMessageClass,
            Jackson2ObjectMapperBuilder objectMapperBuilder,
            ConcurrentWebSocketWriteLimits writeLimits
    ) {
        objectMapper = objectMapperBuilder.build();
        typedSessions = new Hashtable<>();
        this.typedWebSocketHandler = typedWebSocketHandler;
        this.inboundMessageClass = inboundMessageClass;
        this.writeLimits = writeLimits;
    }

    TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> getSessionAndState(WebSocketSession session) {
        return this.typedSessions.get(session.getId());
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        TypedWebSocketSession<OutboundMessage> runnerSession = new TypedWebSocketSession<>(
                session, objectMapper, writeLimits);
        SessionState state = typedWebSocketHandler.handleConnectionEstablished(runnerSession);
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState =
                new TypedWebSocketSessionAndState<>(runnerSession, state);
        typedSessions.put(session.getId(), sessionAndState);
    }

    @Override
    protected void handleBinaryMessage(@NonNull WebSocketSession session, @NonNull BinaryMessage binaryMessage) throws Exception {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.get(session.getId());
        InboundMessage message =
                objectMapper.readValue(binaryMessage.getPayload().array(), this.inboundMessageClass);
        typedWebSocketHandler.handleMessage(sessionAndState.getSession(), sessionAndState.getState(), message);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.remove(session.getId());
        Outcome outcome = typedWebSocketHandler.afterConnectionClosed(sessionAndState.getSession(), sessionAndState.getState(), status);
        if (status.equalsCode(CloseStatus.NORMAL)) {
            sessionAndState.setClosedOk(outcome);
        } else {
            sessionAndState.setClosedError(status);
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.get(session.getId());
        typedWebSocketHandler.handleTransportError(sessionAndState.getSession(), sessionAndState.getState(), exception);
    }
}
