package uk.danielgooding.kokaplayground.common;

import org.springframework.web.socket.CloseStatus;

import java.util.concurrent.CompletableFuture;

public class TypedWebSocketSessionAndState<OutboundMessage, State> {
    private final TypedWebSocketSession<OutboundMessage> session;
    private final State state;
    private final CompletableFuture<Void> closed;

    public TypedWebSocketSessionAndState(TypedWebSocketSession<OutboundMessage> session, State state) {
        this.session = session;
        this.state = state;
        this.closed = new CompletableFuture<>();
    }

    public TypedWebSocketSession<OutboundMessage> getSession() {
        return session;
    }

    public State getState() {
        return state;
    }

    public CompletableFuture<Void> getClosed() {
        return closed;
    }

    void setClosed() {
        closed.complete(null);
    }
}
