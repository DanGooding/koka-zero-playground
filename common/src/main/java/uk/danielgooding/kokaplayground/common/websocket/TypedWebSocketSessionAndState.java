package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.web.socket.CloseStatus;

import java.util.concurrent.CompletableFuture;

public class TypedWebSocketSessionAndState<OutboundMessage, State, Outcome> {
    private final ITypedWebSocketSession<OutboundMessage> session;
    private final State state;
    private final CompletableFuture<Outcome> outcomeFuture;

    public TypedWebSocketSessionAndState(ITypedWebSocketSession<OutboundMessage> session, State state) {
        this.session = session;
        this.state = state;
        this.outcomeFuture = new CompletableFuture<>();
    }

    public ITypedWebSocketSession<OutboundMessage> getSession() {
        return session;
    }

    public State getState() {
        return state;
    }

    public CompletableFuture<Outcome> getOutcomeFuture() {
        return outcomeFuture;
    }

    public void setClosedOk(Outcome outcome) {
        outcomeFuture.complete(outcome);
    }

    public void setClosedError(CloseStatus closeStatus) {
        outcomeFuture.completeExceptionally(new RuntimeException(String.format(
                "WebSocket closed with error: %s", closeStatus)));
    }
}
