package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.util.List;

public class StatelessTypedWebSocketHandler<InboundMessage, OutboundMessage, Outcome>
        implements TypedWebSocketHandler<
        InboundMessage, OutboundMessage, StatelessTypedWebSocketHandler.EmptyState, Void, Outcome> {

    private static final EmptyState emptyState = new EmptyState();

    private final IStatelessTypedWebSocketHandler<InboundMessage, OutboundMessage, Outcome> handler;

    public StatelessTypedWebSocketHandler(IStatelessTypedWebSocketHandler<InboundMessage, OutboundMessage, Outcome> handler) {
        this.handler = handler;
    }

    public static class EmptyState implements ISessionState<Void> {
        @Override
        public Void getStateTag() {
            return null;
        }
    }

    @Override
    public EmptyState handleConnectionEstablished(TypedWebSocketSession<OutboundMessage, Outcome> session) throws IOException {
        handler.handleConnectionEstablished(session);
        return emptyState;
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            EmptyState ignored,
            @NonNull InboundMessage inbound) throws IOException {
        handler.handleMessage(session, inbound);
    }

    @Override
    public Iterable<Void> allSessionStateTags() {
        return List.of();
    }

    @Override
    public Outcome afterConnectionClosedOk(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            EmptyState ignored) throws IOException {
        return handler.afterConnectionClosedOk(session);
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<OutboundMessage, Outcome> session,
            EmptyState ignored,
            CloseStatus status) throws IOException {
        handler.afterConnectionClosedErroneously(session, status);
    }

    @Override
    public boolean isServer() {
        return handler.isServer();
    }

    @Override
    public void handleTransportError(TypedWebSocketSession<OutboundMessage, Outcome> session,
                                     EmptyState ignored,
                                     Throwable exception) throws IOException {
        handler.handleTransportError(session, exception);
    }
}
