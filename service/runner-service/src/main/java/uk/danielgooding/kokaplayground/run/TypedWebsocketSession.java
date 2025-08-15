package uk.danielgooding.kokaplayground.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import uk.danielgooding.kokaplayground.common.SessionId;

import java.io.IOException;

public class TypedWebsocketSession<OutboundMessage> {
    private final SessionId id;
    private final ConcurrentWebSocketSessionDecorator session;
    private final ObjectMapper objectMapper;

    public TypedWebsocketSession(
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
        TextMessage reply = new TextMessage(objectMapper.writeValueAsBytes(messageObject));
        session.sendMessage(reply);
    }

    public void close() throws IOException {
        session.close();
    }
}
