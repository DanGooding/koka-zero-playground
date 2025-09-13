package uk.danielgooding.kokaplayground.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Hashtable;

public class UntypedWrapperWebSocketHandler<
        InboundMessage,
        OutboundMessage,
        SessionState extends ISessionState<SessionStateTag>,
        SessionStateTag,
        Outcome> {

    private final Hashtable<String, TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome>>
            typedSessions;
    private final ObjectMapper objectMapper;

    private final TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, SessionStateTag, Outcome>
            typedWebSocketHandler;

    private final Class<InboundMessage> inboundMessageClass;

    private static final Logger logger = LoggerFactory.getLogger(UntypedWrapperWebSocketHandler.class);

    public UntypedWrapperWebSocketHandler(
            TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, SessionStateTag, Outcome>
                    typedWebSocketHandler,
            Class<InboundMessage> inboundMessageClass,
            Jackson2ObjectMapperBuilder objectMapperBuilder,
            MeterRegistry meterRegistry
    ) {
        objectMapper = objectMapperBuilder.build();
        typedSessions = new Hashtable<>();
        this.typedWebSocketHandler = typedWebSocketHandler;
        this.inboundMessageClass = inboundMessageClass;

        if (typedWebSocketHandler.isServer()) {
            for (SessionStateTag stateTag : typedWebSocketHandler.allSessionStateTags()) {
                // TypedWebSocketClient allows creating a new handler instance for each request.
                // So the map here would only contain one session. Therefore, don't report client sessions for now.
                Gauge.builder("websocket_sessions",
                                this,
                                h -> h.getSessionCount(stateTag))
                        .tag("handler", typedWebSocketHandler.getClass().getSimpleName())
                        .tag("role", typedWebSocketHandler.isServer() ? "server" : "client")
                        .tag("state", stateTag.toString())
                        .register(meterRegistry);
            }
        }
    }

    TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> getSessionAndState(WebSocketSession session) {
        return this.typedSessions.get(session.getId());
    }

    public void afterConnectionEstablished(@NonNull IWebSocketSession session) throws IOException {
        TypedWebSocketSession<OutboundMessage, Outcome> runnerSession =
                new TypedWebSocketSession<>(session, objectMapper, typedWebSocketHandler.getClass());

        SessionState state = runUserCode(runnerSession, () ->
                typedWebSocketHandler.handleConnectionEstablished(runnerSession));

        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState =
                new TypedWebSocketSessionAndState<>(runnerSession, state);
        typedSessions.put(session.getId(), sessionAndState);
    }

    void closeUserExn(TypedWebSocketSession<OutboundMessage, Outcome> session, Throwable exn) throws IOException {
        session.closeExn(
                String.format("exception in websocket handler %s[%s]",
                        typedWebSocketHandler.getClass().getSimpleName(),
                        session.getId()),
                exn);
    }

    public void handleTextMessage(@NonNull IWebSocketSession session, @NonNull TextMessage textMessage) throws IOException {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState =
                typedSessions.get(session.getId());

        // if already closed before we processed this message
        if (sessionAndState == null) return;

        InboundMessage message =
                objectMapper.readValue(textMessage.getPayload(), this.inboundMessageClass);

        logger.debug("{} received {}", session, message);

        try {
            runUserCode(sessionAndState.getSession(), () ->
                    typedWebSocketHandler.handleMessage(
                            sessionAndState.getSession(), sessionAndState.getState(), message));
        } catch (Throwable e) {
            closeUserExn(sessionAndState.getSession(), e);
        }
    }

    public void afterConnectionClosed(@NonNull IWebSocketSession session, @NonNull CloseStatus status) throws IOException {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState =
                typedSessions.remove(session.getId());
        if (sessionAndState == null) return;

        sessionAndState.getSession().getOutcomeFuture().whenComplete((ignored, exn) -> {
            if (exn != null) {
                logger.error("[{}]{} websocket closed with error",
                        typedWebSocketHandler.getClass().getSimpleName(), sessionAndState.getSession(), exn);
            }
        });

        // If the user initiated the close, don't call their afterConnectionClosed handlers.
        // They will have already completed the outcome future.
        if (!sessionAndState.getSession().wasClosedByThisSide()) {
            // close was driven by the transport layer, or the peer.

            // The server doesn't consider a client disconnect as an error.
            // A client however expects the server to cleanly close the connection
            // (unless the client closes itself first).
            if (typedWebSocketHandler.isServer() || status.equalsCode(CloseStatus.NORMAL)) {
                try {
                    Outcome outcome = runUserCode(sessionAndState.getSession(), () ->
                            typedWebSocketHandler.afterConnectionClosedOk(
                                    sessionAndState.getSession(), sessionAndState.getState()));

                    sessionAndState.getSession().closeOk(outcome);
                } catch (Throwable e) {
                    closeUserExn(sessionAndState.getSession(), e);
                }
            } else {
                try {
                    runUserCode(sessionAndState.getSession(), () ->
                            typedWebSocketHandler.afterConnectionClosedErroneously(
                                    sessionAndState.getSession(), sessionAndState.getState(), status));

                    sessionAndState.getSession().closeErrorStatus(
                            String.format("[%s] connection closed with error", session.getId()),
                            status);
                } catch (Throwable e) {
                    closeUserExn(sessionAndState.getSession(), e);
                }
            }
        }
    }

    public void handleTransportError(@NonNull IWebSocketSession session, @NonNull Throwable exception) throws IOException {
        TypedWebSocketSessionAndState<OutboundMessage, SessionState, Outcome> sessionAndState = typedSessions.get(session.getId());
        if (sessionAndState == null) return;

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

    private <T> T runUserCode(TypedWebSocketSession<OutboundMessage, Outcome> session, Run<T> userCode) throws IOException {
        if (typedWebSocketHandler.isServer()) {
            try {
                WebsocketServerSessionHolder.getInstance().setSession(session);
                return userCode.run();
            } finally {
                WebsocketServerSessionHolder.getInstance().clearSession();
            }
        } else {
            return userCode.run();
        }
    }

    private void runUserCode(TypedWebSocketSession<OutboundMessage, Outcome> session, RunNoResult userCode) throws IOException {
        Void ignored = runUserCode(session, () -> {
            userCode.run();
            return null;
        });
    }

    private double getSessionCount(SessionStateTag stateTag) {
        int count = 0;
        for (var sessionAndState : typedSessions.values()) {
            if (sessionAndState.getState().getStateTag() == stateTag) {
                count++;
            }
        }
        return count;
    }

    @FunctionalInterface
    private interface RunNoResult {
        void run() throws IOException;
    }

    @FunctionalInterface
    private interface Run<T> {
        T run() throws IOException;
    }
}
