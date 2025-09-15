package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;

public class RealWebSocketSession implements IWebSocketSession {
    private final WebSocketSession webSocketSession;

    public RealWebSocketSession(WebSocketSession webSocketSession, ConcurrentWebSocketWriteLimits writeLimits) {
        this.webSocketSession =
                new ConcurrentWebSocketSessionDecorator(
                        webSocketSession, writeLimits.sendTimeLimitMs(), writeLimits.bufferSizeLimitBytes());
    }

    @Override
    public String getId() {
        return webSocketSession.getId();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return webSocketSession.getAttributes();
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) throws IOException {
        webSocketSession.sendMessage(message);
    }

    @Override
    public void close(CloseStatus closeStatus) throws IOException {
        webSocketSession.close(closeStatus);
    }
}
