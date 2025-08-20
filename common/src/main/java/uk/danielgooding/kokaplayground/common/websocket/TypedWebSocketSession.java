package uk.danielgooding.kokaplayground.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/// this manages a websocket session lifecycle
/// it can send typed messages
/// and close, setting the resulting Outcome or error
public class TypedWebSocketSession<OutboundMessage, Outcome> {
    private final SessionId id;
    private final IWebSocketSession session;
    private final ObjectMapper objectMapper;
    private final CompletableFuture<Outcome> outcomeFuture;

    public TypedWebSocketSession(
            IWebSocketSession session,
            ObjectMapper objectMapper) {
        this.session = session;
        this.id = new SessionId(session.getId());
        this.objectMapper = objectMapper;
        outcomeFuture = new CompletableFuture<>();
    }

    public SessionId getId() {
        return this.id;
    }

    public void sendMessage(OutboundMessage messageObject) throws IOException {
        TextMessage reply = new TextMessage(objectMapper.writeValueAsBytes(messageObject));
        session.sendMessage(reply);
    }

    public void closeOk(Outcome outcome) throws IOException {
        session.close(CloseStatus.NORMAL);
        outcomeFuture.complete(outcome);
    }

    public void closeError(CloseStatus closeStatus) throws IOException {
        session.close(closeStatus);
        outcomeFuture.completeExceptionally(new RuntimeException(
                String.format("websocket connection closed with error: %s", closeStatus)));
    }

    public void closeUserExn(Throwable exn) throws IOException {
        outcomeFuture.completeExceptionally(new RuntimeException("user code raised into WebSocket handler", exn));
        session.close(CloseStatus.SERVER_ERROR);
    }

    public CompletableFuture<Outcome> getOutcomeFuture() {
        return outcomeFuture;
    }
}
