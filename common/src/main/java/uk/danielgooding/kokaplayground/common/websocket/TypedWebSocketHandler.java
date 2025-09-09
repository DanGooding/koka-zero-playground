package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;

public interface TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> {

    SessionState handleConnectionEstablished(
            TypedWebSocketSession<OutboundMessage, Outcome> session
    ) throws IOException;

    void handleMessage(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            SessionState sessionState,
            @NonNull InboundMessage inbound
    ) throws IOException;

    Outcome afterConnectionClosedOk(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            SessionState sessionState
    ) throws IOException;

    void afterConnectionClosedErroneously(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            SessionState sessionState,
            CloseStatus status
    ) throws IOException;

    boolean isServer();

    void handleTransportError(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            SessionState sessionState,
            Throwable exception
    ) throws IOException;
}
