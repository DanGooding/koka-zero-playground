package uk.danielgooding.kokaplayground.common.websocket;

import java.util.concurrent.CompletableFuture;

public class TypedWebSocketSessionAndState<OutboundMessage, State, Outcome> {
    private final TypedWebSocketSession<OutboundMessage> session;
    private final State state;
    private final CompletableFuture<Outcome> outcomeFuture;

    public TypedWebSocketSessionAndState(TypedWebSocketSession<OutboundMessage> session, State state) {
        this.session = session;
        this.state = state;
        this.outcomeFuture = new CompletableFuture<>();
    }

    public TypedWebSocketSession<OutboundMessage> getSession() {
        return session;
    }

    public State getState() {
        return state;
    }

    public CompletableFuture<Outcome> getOutcomeFuture() {
        return outcomeFuture;
    }

    void setClosed(Outcome outcome) {
        outcomeFuture.complete(outcome);
    }
}
