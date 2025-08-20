package uk.danielgooding.kokaplayground.common.websocket;

public class TypedWebSocketSessionAndState<OutboundMessage, State, Outcome> {
    private final TypedWebSocketSession<OutboundMessage, Outcome> session;
    private final State state;

    public TypedWebSocketSessionAndState(TypedWebSocketSession<OutboundMessage, Outcome> session, State state) {
        this.session = session;
        this.state = state;
    }

    public TypedWebSocketSession<OutboundMessage, Outcome> getSession() {
        return session;
    }

    public State getState() {
        return state;
    }
}
