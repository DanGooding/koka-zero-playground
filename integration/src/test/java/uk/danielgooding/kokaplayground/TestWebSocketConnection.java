package uk.danielgooding.kokaplayground;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.websocket.*;

import java.io.IOException;

class TestSession<Inbound, Outbound, State, Outcome> implements IWebSocketSession {
    private final SessionId sessionId;
    private boolean isClosed = false;
    private UntypedWrapperWebSocketHandler<Inbound, Outbound, State, Outcome> myHandler;
    private Callback<WebSocketMessage<?>> peerHandleMessage;
    private Callback<CloseStatus> peerClose;

    TestSession(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    public void init(UntypedWrapperWebSocketHandler<Inbound, Outbound, State, Outcome> myHandler,
                     Callback<WebSocketMessage<?>> peerHandleMessage,
                     Callback<CloseStatus> peerClose) {
        this.myHandler = myHandler;

        this.peerHandleMessage = peerHandleMessage;
        this.peerClose = peerClose;
    }

    @Override
    public String getId() {
        return this.sessionId.toString();
    }

    @Override
    public void sendMessage(WebSocketMessage<?> messageObject) throws IOException {
        if (isClosed) {
            throw new IOException("Session is closed - cannot sendMessage");
        }
        peerHandleMessage.call(messageObject);
    }

    @Override
    public void close(CloseStatus closeStatus) throws IOException {
        if (isClosed) return;
        isClosed = true;

        myHandler.afterConnectionClosed(this, closeStatus);
        peerClose.call(closeStatus);
    }
}

/// for now this class mocks a single websocket connection
class TestWebSocketConnection<Inbound, Outbound, ClientState, ServerState, ClientOutcome> {

    private final UntypedWrapperWebSocketHandler<Inbound, Outbound, ServerState, Void> serverHandler;
    private final UntypedWrapperWebSocketHandler<Outbound, Inbound, ClientState, ClientOutcome> clientHandler;

    private final SessionId sessionId;

    TestWebSocketConnection(
            TypedWebSocketHandler<Inbound, Outbound, ServerState, Void> serverHandler,
            TypedWebSocketHandler<Outbound, Inbound, ClientState, ClientOutcome> clientHandler,
            Class<Inbound> inboundClass, Class<Outbound> outboundClass,
            SessionId sessionId) {
        Jackson2ObjectMapperBuilder objectMapperBuilder = new Jackson2ObjectMapperBuilder();
        this.serverHandler = new UntypedWrapperWebSocketHandler<>(
                serverHandler, inboundClass, objectMapperBuilder);
        this.clientHandler = new UntypedWrapperWebSocketHandler<>(
                clientHandler, outboundClass, objectMapperBuilder);
        this.sessionId = sessionId;
    }

    public void establishConnection() throws IOException {
        TestSession<Inbound, Outbound, ServerState, Void> serverSession =
                new TestSession<>(sessionId);

        TestSession<Outbound, Inbound, ClientState, ClientOutcome> clientSession =
                new TestSession<>(sessionId);

        serverHandler.afterConnectionEstablished(serverSession);
        clientHandler.afterConnectionEstablished(clientSession);

        serverSession.init(
                serverHandler,
                message -> {
                    if (message instanceof TextMessage textMessage) {
                        clientHandler.handleTextMessage(clientSession, textMessage);
                    } else {
                        throw new UnsupportedOperationException("Binary message not implemented in websocket test");
                    }
                },
                clientSession::close);

        clientSession.init(
                clientHandler,
                message -> {
                    if (message instanceof TextMessage textMessage) {
                        serverHandler.handleTextMessage(clientSession, textMessage);
                    } else {
                        throw new UnsupportedOperationException("Binary message not implemented in websocket test");
                    }
                },
                serverSession::close);
    }

    public TypedWebSocketSessionAndState<Inbound, ClientState, ClientOutcome> getClientSessionAndState() {
        return clientHandler.getSessionAndState(sessionId);
    }

    public void breakConnection() throws Exception {
        clientHandler.getSessionAndState(sessionId).getSession().closeError(CloseStatus.NO_CLOSE_FRAME);
        serverHandler.getSessionAndState(sessionId).getSession().closeError(CloseStatus.NO_CLOSE_FRAME);
    }

}