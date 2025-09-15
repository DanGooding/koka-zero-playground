package uk.danielgooding.kokaplayground.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// this manages a websocket session lifecycle
/// it can send typed messages
/// and close, setting the resulting Outcome or error
public class TypedWebSocketSession<OutboundMessage, Outcome> {
    private final SessionId id;
    private final IWebSocketSession session;
    private final ObjectMapper objectMapper;
    private final Class<?> handlerClass;
    private final CompletableFuture<Outcome> outcomeFuture;
    private boolean closedByThisSide = false;

    private static final Logger logger = LoggerFactory.getLogger(TypedWebSocketSession.class);

    public TypedWebSocketSession(
            IWebSocketSession session,
            ObjectMapper objectMapper,
            Class<?> handlerClass) {
        this.session = session;
        this.id = new SessionId(session.getId());
        this.objectMapper = objectMapper;
        outcomeFuture = new CompletableFuture<>();
        this.handlerClass = handlerClass;
    }

    public SessionId getId() {
        return this.id;
    }

    public Map<String, Object> getAttributes() {
        return session.getAttributes();
    }

    public void sendMessage(OutboundMessage messageObject) throws IOException {
        logger.debug("sending {} (session {}", messageObject, session.getId());
        TextMessage reply = new TextMessage(objectMapper.writeValueAsBytes(messageObject));
        session.sendMessage(reply);
    }

    public void closeOk(Outcome outcome) throws IOException {
        closedByThisSide = true;
        session.close(CloseStatus.NORMAL);
        outcomeFuture.complete(outcome);
    }

    public void closeGoingAway(Outcome outcome) throws IOException {
        closedByThisSide = true;
        session.close(CloseStatus.GOING_AWAY);
        outcomeFuture.complete(outcome);
    }

    public void closeExn(String message, Throwable exn) throws IOException {
        closedByThisSide = true;
        outcomeFuture.completeExceptionally(exn);
        session.close(CloseStatus.SERVER_ERROR);
    }

    public void closeErrorStatus(String message, CloseStatus closeStatus) throws IOException {
        closedByThisSide = true;
        outcomeFuture.completeExceptionally(new RuntimeException(
                String.format("websocket closing '%s': code %s", message, closeStatus)));
        session.close(closeStatus);
    }

    public CompletableFuture<Outcome> getOutcomeFuture() {
        return outcomeFuture;
    }

    public IWebSocketSession getRawSession() {
        return session;
    }

    public boolean wasClosedByThisSide() {
        return closedByThisSide;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", handlerClass.getSimpleName(), session.getId());
    }
}
