package uk.danielgooding.kokaplayground.run;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;

public interface TypedWebSocketHandler<InboundMessage, OutboundMessage> {

    void handleConnectionEstablished(TypedWebsocketSession<OutboundMessage> session) throws Exception;

    void handleMessage(TypedWebsocketSession<OutboundMessage> session, @NonNull InboundMessage inbound) throws Exception;

    void afterConnectionClosed(TypedWebsocketSession<OutboundMessage> session, CloseStatus status) throws Exception;

    void handleTransportError(TypedWebsocketSession<OutboundMessage> session, Throwable exception) throws Exception;
}
