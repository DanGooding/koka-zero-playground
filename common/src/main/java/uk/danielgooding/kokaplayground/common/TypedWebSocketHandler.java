package uk.danielgooding.kokaplayground.common;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;

public interface TypedWebSocketHandler<InboundMessage, OutboundMessage, SessionState> {

    SessionState handleConnectionEstablished(TypedWebSocketSession<OutboundMessage> session) throws Exception;

    void handleMessage(TypedWebSocketSession<OutboundMessage> session, SessionState sessionState, @NonNull InboundMessage inbound) throws Exception;

    void afterConnectionClosed(TypedWebSocketSession<OutboundMessage> session, SessionState sessionState, CloseStatus status) throws Exception;

    void handleTransportError(TypedWebSocketSession<OutboundMessage> session, SessionState sessionState, Throwable exception) throws Exception;
}
