package uk.danielgooding.kokaplayground.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Hashtable;
import java.util.concurrent.Callable;

public class UntypedWrapperWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> extends TextWebSocketHandler {

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

    /// give the user's exception back to them by setting the sessionAndState outcome future.
    /// this avoids the user code hanging forever
    /// and means the outcome future will have a more meaningful error
    /// (instead of just having the websocket exit code)
    <T> T runUserCode(
            TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState,
            Callable<T> userCode
    ) throws Exception {
        try {
            return userCode.call();
        } catch (Throwable e) {
            sessionAndState.setClosedUserExn(e);
            throw e;
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage textMessage) throws Exception {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.get(session.getId());
        InboundMessage message =
                objectMapper.readValue(textMessage.getPayload(), this.inboundMessageClass);

        runUserCode(sessionAndState, () -> {
            typedWebSocketHandler.handleMessage(sessionAndState.getSession(), sessionAndState.getState(), message);
            return null;
        });
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.remove(session.getId());
        if (status.equalsCode(CloseStatus.NORMAL)) {
            Outcome outcome = runUserCode(sessionAndState, () ->
                    typedWebSocketHandler.afterConnectionClosedOk(
                            sessionAndState.getSession(), sessionAndState.getState()));
            sessionAndState.setClosedOk(outcome);
        } else {

            runUserCode(sessionAndState, () -> {
                typedWebSocketHandler.afterConnectionClosedErroneously(
                        sessionAndState.getSession(), sessionAndState.getState(), status);
                return null;
            });
            sessionAndState.setClosedError(status);
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.get(session.getId());

        runUserCode(sessionAndState, () -> {
            typedWebSocketHandler.handleTransportError(
                    sessionAndState.getSession(), sessionAndState.getState(), exception);
            return null;
        });
    }
}
