package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;

import java.io.IOException;

public interface IWebSocketSession {

    String getId();

    void sendMessage(WebSocketMessage<?> message) throws IOException;

    void close(CloseStatus closeStatus) throws IOException;
}
