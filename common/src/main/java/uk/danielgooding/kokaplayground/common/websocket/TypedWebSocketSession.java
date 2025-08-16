package uk.danielgooding.kokaplayground.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;

public class TypedWebSocketSession<OutboundMessage> {
    private final SessionId id;
    private final ConcurrentWebSocketSessionDecorator session;
    private final ObjectMapper objectMapper;

    public TypedWebSocketSession(
            WebSocketSession session,
            ObjectMapper objectMapper,
            ConcurrentWebSocketWriteLimits writeLimits) {
        this.session =
                new ConcurrentWebSocketSessionDecorator(
                        session, writeLimits.sendTimeLimitMs(), writeLimits.bufferSizeLimitBytes());
        this.id = new SessionId(session.getId());
        this.objectMapper = objectMapper;
    }

    public SessionId getId() {
        return this.id;
    }

    public void sendMessage(OutboundMessage messageObject) throws IOException {
        BinaryMessage reply = new BinaryMessage(objectMapper.writeValueAsBytes(messageObject));
        session.sendMessage(reply);
    }

    public void close() throws IOException {
        session.close();
    }
}
