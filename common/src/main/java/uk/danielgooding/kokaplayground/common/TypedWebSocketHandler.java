package uk.danielgooding.kokaplayground.common;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;

public interface TypedWebSocketHandler<InboundMessage, OutboundMessage> {

    void handleConnectionEstablished(TypedWebSocketSession<OutboundMessage> session) throws Exception;

    void handleMessage(TypedWebSocketSession<OutboundMessage> session, @NonNull InboundMessage inbound) throws Exception;

    void afterConnectionClosed(TypedWebSocketSession<OutboundMessage> session, CloseStatus status) throws Exception;

    void handleTransportError(TypedWebSocketSession<OutboundMessage> session, Throwable exception) throws Exception;
}
