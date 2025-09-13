package uk.danielgooding.kokaplayground;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.websocket.*;

import java.io.IOException;

class TestSession<Inbound, Outbound, State extends ISessionState<StateTag>, StateTag, Outcome>
        implements IWebSocketSession {
    private final SessionId sessionId;
    private boolean isClosed = false;
    private UntypedWrapperWebSocketHandler<Inbound, Outbound, State, StateTag, Outcome> myHandler;
    private Callback<WebSocketMessage<?>> peerHandleMessage;
    private Callback<CloseStatus> peerClose;

    TestSession(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    public void init(UntypedWrapperWebSocketHandler<Inbound, Outbound, State, StateTag, Outcome> myHandler,
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
class TestWebSocketConnection<
        Inbound,
        Outbound,
        ClientState extends ISessionState<ClientStateTag>, ClientStateTag,
        ServerState extends ISessionState<ServerStateTag>, ServerStateTag,
        ClientOutcome> {

    private final UntypedWrapperWebSocketHandler<Inbound, Outbound, ServerState, ServerStateTag, Void>
            serverHandler;
    private final UntypedWrapperWebSocketHandler<Outbound, Inbound, ClientState, ClientStateTag, ClientOutcome>
            clientHandler;

    private final SessionId sessionId;

    TestWebSocketConnection(
            TypedWebSocketHandler<Inbound, Outbound, ServerState, ServerStateTag, Void> serverHandler,
            TypedWebSocketHandler<Outbound, Inbound, ClientState, ClientStateTag, ClientOutcome> clientHandler,
            Class<Inbound> inboundClass, Class<Outbound> outboundClass,
            SessionId sessionId) {
        Jackson2ObjectMapperBuilder objectMapperBuilder = new Jackson2ObjectMapperBuilder();

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        this.serverHandler = new UntypedWrapperWebSocketHandler<>(
                serverHandler, inboundClass, objectMapperBuilder, meterRegistry);
        this.clientHandler = new UntypedWrapperWebSocketHandler<>(
                clientHandler, outboundClass, objectMapperBuilder, meterRegistry);
        this.sessionId = sessionId;
    }

    public void establishConnection() throws IOException {
        TestSession<Inbound, Outbound, ServerState, ServerStateTag, Void> serverSession =
                new TestSession<>(sessionId);

        TestSession<Outbound, Inbound, ClientState, ClientStateTag, ClientOutcome> clientSession =
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

    public TypedWebSocketSessionAndState<Outbound, ServerState, Void> getServerSessionAndState() {
        return serverHandler.getSessionAndState(sessionId);
    }

    public void breakConnection() throws Exception {
        clientHandler.afterConnectionClosed(
                clientHandler.getSessionAndState(sessionId).getSession().getRawSession(),
                CloseStatus.NO_CLOSE_FRAME);

        clientHandler.afterConnectionClosed(
                clientHandler.getSessionAndState(sessionId).getSession().getRawSession(),
                CloseStatus.NO_CLOSE_FRAME);
    }

}