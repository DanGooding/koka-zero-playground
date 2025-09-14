package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Hashtable;

/// this is a tiny wrapper around UntypedWrapperWebSocketHandler,
/// which simply wraps the WebSocketSession objects such that they fit the IWebSocketSession interface.
/// this runs in prod, but not in tests, since tests provide mock implementations fitting that interface
public class RealWebSocketHandler extends TextWebSocketHandler {
    private final UntypedWrapperWebSocketHandler<?, ?, ?, ?, ?> handler;
    private final ConcurrentWebSocketWriteLimits writeLimits;
    private final Hashtable<String, IWebSocketSession> decoratedSessions;

    public RealWebSocketHandler(UntypedWrapperWebSocketHandler<?, ?, ?, ?, ?> handler, ConcurrentWebSocketWriteLimits writeLimits) {
        this.handler = handler;
        this.writeLimits = writeLimits;
        decoratedSessions = new Hashtable<>();
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        IWebSocketSession decoratedSession =
                new RealWebSocketSession(session, writeLimits);
        decoratedSessions.put(session.getId(), decoratedSession);

        handler.afterConnectionEstablished(decoratedSession);
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        IWebSocketSession decoratedSession = decoratedSessions.get(session.getId());
        if (decoratedSession != null) handler.handleTextMessage(decoratedSession, message);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        IWebSocketSession decoratedSession = decoratedSessions.remove(session.getId());
        handler.afterConnectionClosed(decoratedSession, status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        IWebSocketSession decoratedSession = decoratedSessions.get(session.getId());
        if (decoratedSession != null) handler.handleTransportError(decoratedSession, exception);
    }
}
