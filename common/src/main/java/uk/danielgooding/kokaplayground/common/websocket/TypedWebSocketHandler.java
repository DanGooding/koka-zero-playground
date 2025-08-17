package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;

public interface TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState, Outcome> {

    SessionState handleConnectionEstablished(ITypedWebSocketSession<OutboundMessage> session) throws Exception;

    void handleMessage(ITypedWebSocketSession<OutboundMessage> session, SessionState sessionState, @NonNull InboundMessage inbound) throws Exception;

    Outcome afterConnectionClosed(ITypedWebSocketSession<OutboundMessage> session, SessionState sessionState, CloseStatus status) throws Exception;

    void handleTransportError(ITypedWebSocketSession<OutboundMessage> session, SessionState sessionState, Throwable exception) throws Exception;
}
