package uk.danielgooding.kokaplayground;

import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.SessionId;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.compileandrun.CollectingRunnerClientWebSocketHandler;
import uk.danielgooding.kokaplayground.compileandrun.CollectingRunnerClientWebSocketState;
import uk.danielgooding.kokaplayground.protocol.RunStream;
import uk.danielgooding.kokaplayground.run.RunnerSessionState;
import uk.danielgooding.kokaplayground.run.RunnerWebSocketHandler;

import java.io.IOException;

class TestSession<Inbound, Outbound, State, Outcome> implements ITypedWebSocketSession<Outbound> {
    private final SessionId sessionId;
    private boolean isClosed = false;
    private TypedWebSocketHandler<Inbound, Outbound, State, Outcome> myHandler;
    private TypedWebSocketSessionAndState<Outbound, State, Outcome> mySessionAndState;
    private Callback<Outbound> peerHandleMessage;
    private Callback<CloseStatus> peerClose;

    TestSession(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    public void init(TypedWebSocketHandler<Inbound, Outbound, State, Outcome> myHandler,
                     TypedWebSocketSessionAndState<Outbound, State, Outcome> mySessionAndState,
                     Callback<Outbound> peerHandleMessage,
                     Callback<CloseStatus> peerClose) {
        this.myHandler = myHandler;
        this.mySessionAndState = mySessionAndState;
        this.peerHandleMessage = peerHandleMessage;
        this.peerClose = peerClose;
    }

    @Override
    public SessionId getId() {
        return this.sessionId;
    }

    @Override
    public void sendMessage(Outbound messageObject) throws Exception {
        if (isClosed) {
            System.out.println("tried to send on closed Server");
            throw new IOException("ServerSession is closed - cannot sendMessage");
        }
        peerHandleMessage.call(messageObject);
    }

    @Override
    public void closeOk() throws Exception {
        close(CloseStatus.NORMAL);
    }

    @Override
    public void close(CloseStatus closeStatus) throws Exception {
        if (isClosed) return;
        isClosed = true;
        if (closeStatus.equalsCode(CloseStatus.NORMAL)) {
            Outcome outcome = myHandler.afterConnectionClosedOk(
                    mySessionAndState.getSession(),
                    mySessionAndState.getState()
            );
            mySessionAndState.setClosedOk(outcome);
        } else {
            myHandler.afterConnectionClosedErroneously(
                    mySessionAndState.getSession(), mySessionAndState.getState(), closeStatus);
            mySessionAndState.setClosedError(closeStatus);
        }
        peerClose.call(closeStatus);
    }
}


/// for now this class mocks a single websocket connection
class TestWebSocketConnection {

    private final RunnerWebSocketHandler serverHandler;
    private final CollectingRunnerClientWebSocketHandler clientHandler;
    private final SessionId sessionId;

    private TypedWebSocketSessionAndState<RunStream.Outbound.Message, RunnerSessionState, Void> serverSessionAndState;
    private TypedWebSocketSessionAndState<RunStream.Inbound.Message, CollectingRunnerClientWebSocketState, OrError<String>> clientSessionAndState;

    TestWebSocketConnection(RunnerWebSocketHandler serverHandler, CollectingRunnerClientWebSocketHandler clientHandler, SessionId sessionId) {
        this.serverHandler = serverHandler;
        this.clientHandler = clientHandler;
        this.sessionId = sessionId;
    }

    public void establishConnection() {
        TestSession<RunStream.Inbound.Message, RunStream.Outbound.Message,
                RunnerSessionState, Void> serverSession =
                new TestSession<>(sessionId);

        TestSession<RunStream.Outbound.Message, RunStream.Inbound.Message,
                CollectingRunnerClientWebSocketState, OrError<String>> clientSession =
                new TestSession<>(sessionId);

        RunnerSessionState serverState = serverHandler.handleConnectionEstablished(serverSession);
        CollectingRunnerClientWebSocketState clientState = clientHandler.handleConnectionEstablished(clientSession);

        serverSessionAndState = new TypedWebSocketSessionAndState<>(serverSession, serverState);
        clientSessionAndState = new TypedWebSocketSessionAndState<>(clientSession, clientState);

        serverSession.init(
                serverHandler,
                serverSessionAndState,
                message -> clientHandler.handleMessage(clientSession, clientState, message),
                clientSession::close);

        clientSession.init(
                clientHandler,
                clientSessionAndState,
                message -> serverHandler.handleMessage(serverSession, serverState, message),
                serverSession::close);
    }

    public TypedWebSocketSessionAndState<RunStream.Inbound.Message, CollectingRunnerClientWebSocketState, OrError<String>> getClientSessionAndState() {
        return clientSessionAndState;
    }

    public void breakConnection() throws Exception {
        clientSessionAndState.getSession().close(CloseStatus.NO_CLOSE_FRAME);
        serverSessionAndState.getSession().close(CloseStatus.NO_CLOSE_FRAME);
    }

}