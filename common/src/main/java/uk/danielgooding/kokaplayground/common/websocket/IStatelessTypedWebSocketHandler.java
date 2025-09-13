package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;

public interface IStatelessTypedWebSocketHandler<InboundMessage, OutboundMessage, Outcome> {
    void handleConnectionEstablished(
            TypedWebSocketSession<OutboundMessage, Outcome> session
    ) throws IOException;

    void handleMessage(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            @NonNull InboundMessage inbound
    ) throws IOException;

    Outcome afterConnectionClosedOk(
            TypedWebSocketSession<OutboundMessage, Outcome> session
    ) throws IOException;

    void afterConnectionClosedErroneously(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            CloseStatus status
    ) throws IOException;

    boolean isServer();

    void handleTransportError(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            Throwable exception
    ) throws IOException;
}
