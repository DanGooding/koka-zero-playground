package uk.danielgooding.kokaplayground.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Hashtable;

public class UntypedWrapperWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> {

    private final Hashtable<String, TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome>> typedSessions;
    private final ObjectMapper objectMapper;

    private final TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> typedWebSocketHandler;

    private final Class<InboundMessage> inboundMessageClass;

    private static final Log log = LogFactory.getLog(UntypedWrapperWebSocketHandler.class);

    public UntypedWrapperWebSocketHandler(
            TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> typedWebSocketHandler,
            Class<InboundMessage> inboundMessageClass,
            Jackson2ObjectMapperBuilder objectMapperBuilder

    ) {
        objectMapper = objectMapperBuilder.build();
        typedSessions = new Hashtable<>();
        this.typedWebSocketHandler = typedWebSocketHandler;
        this.inboundMessageClass = inboundMessageClass;
    }

    TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> getSessionAndState(WebSocketSession session) {
        return this.typedSessions.get(session.getId());
    }

    public void afterConnectionEstablished(@NonNull IWebSocketSession session) throws IOException {
        TypedWebSocketSession<OutboundMessage, Outcome> runnerSession =
                new TypedWebSocketSession<>(session, objectMapper);

        SessionState state = typedWebSocketHandler.handleConnectionEstablished(runnerSession);
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState =
                new TypedWebSocketSessionAndState<>(runnerSession, state);
        typedSessions.put(session.getId(), sessionAndState);
    }

    void closeUserExn(TypedWebSocketSession<OutboundMessage, Outcome> session, Throwable exn) throws IOException {
        session.closeExn(
                String.format("exception in websocket handler %s for session %s",
                        typedWebSocketHandler.getClass(),
                        session.getId()),
                exn);
    }

    public void handleTextMessage(@NonNull IWebSocketSession session, @NonNull TextMessage textMessage) throws IOException {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.get(session.getId());
        InboundMessage message =
                objectMapper.readValue(textMessage.getPayload(), this.inboundMessageClass);

        try {
            typedWebSocketHandler.handleMessage(sessionAndState.getSession(), sessionAndState.getState(), message);
        } catch (IOException e) {
            closeUserExn(sessionAndState.getSession(), e);
        }
    }

    public void afterConnectionClosed(@NonNull IWebSocketSession session, @NonNull CloseStatus status) throws IOException {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.remove(session.getId());

        sessionAndState.getSession().getOutcomeFuture().whenComplete((ignored, exn) -> {
            log.error(String.format("websocket connection %s closed with error", typedWebSocketHandler.getClass()), exn);
        });

        if (status.equalsCode(CloseStatus.NORMAL)) {
            try {
                Outcome outcome =
                        typedWebSocketHandler.afterConnectionClosedOk(
                                sessionAndState.getSession(), sessionAndState.getState());
                sessionAndState.getSession().closeOk(outcome);
            } catch (Throwable e) {
                closeUserExn(sessionAndState.getSession(), e);
            }
        } else {
            try {
                typedWebSocketHandler.afterConnectionClosedErroneously(
                        sessionAndState.getSession(), sessionAndState.getState(), status);
                sessionAndState.getSession().closeErrorStatus(
                        String.format("websocket connection %s closed with error", session.getId()),
                        status);
            } catch (Throwable e) {
                closeUserExn(sessionAndState.getSession(), e);
            }
        }
    }

    public void handleTransportError(@NonNull IWebSocketSession session, @NonNull Throwable exception) throws IOException {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.get(session.getId());

        try {
            typedWebSocketHandler.handleTransportError(
                    sessionAndState.getSession(), sessionAndState.getState(), exception);

        } catch (Throwable e) {
            closeUserExn(sessionAndState.getSession(), e);
        }
    }

    public TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> getSessionAndState(SessionId sessionId) {
        return typedSessions.get(sessionId.toString());
    }
}
