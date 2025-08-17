package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.web.socket.CloseStatus;

import java.io.IOException;


public interface ITypedWebSocketSession<OutboundMessage> {

    SessionId getId();

    void sendMessage(OutboundMessage messageObject) throws IOException;

    void closeOk() throws IOException;

    void close(CloseStatus closeStatus) throws IOException;
}
