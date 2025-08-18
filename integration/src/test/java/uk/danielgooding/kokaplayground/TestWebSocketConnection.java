package uk.danielgooding.kokaplayground;

import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.SessionId;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.compileandrun.RunnerClientWebSocketHandler;
import uk.danielgooding.kokaplayground.compileandrun.RunnerClientWebSocketState;
import uk.danielgooding.kokaplayground.protocol.RunStreamInbound;
import uk.danielgooding.kokaplayground.protocol.RunStreamOutbound;
import uk.danielgooding.kokaplayground.run.RunnerSessionState;
import uk.danielgooding.kokaplayground.run.RunnerWebSocketHandler;

import java.io.IOException;

/// for now this class mocks a single websocket connection
class TestWebSocketConnection {

    private final RunnerWebSocketHandler serverHandler;
    private final RunnerClientWebSocketHandler clientHandler;
    private final SessionId sessionId;

    private TypedWebSocketSessionAndState<RunStreamOutbound.Message, RunnerSessionState, Void> serverSessionAndState;
    private TypedWebSocketSessionAndState<RunStreamInbound.Message, RunnerClientWebSocketState, OrError<String>> clientSessionAndState;

    TestWebSocketConnection(RunnerWebSocketHandler serverHandler, RunnerClientWebSocketHandler clientHandler, SessionId sessionId) {
        this.serverHandler = serverHandler;
        this.clientHandler = clientHandler;
        this.sessionId = sessionId;
    }

    class ServerSession implements ITypedWebSocketSession<RunStreamOutbound.Message> {
        private boolean isClosed = false;

        @Override
        public SessionId getId() {
            return TestWebSocketConnection.this.sessionId;
        }

        @Override
        public void sendMessage(RunStreamOutbound.Message messageObject) throws IOException {
            if (isClosed) {
                System.out.println("tried to send on closed Server");
                throw new IOException("ServerSession is closed - cannot sendMessage");
            }
            TestWebSocketConnection.this.clientHandler.handleMessage(
                    clientSessionAndState.getSession(),
                    clientSessionAndState.getState(),
                    messageObject);
        }

        @Override
        public void closeOk() throws IOException {
            close(CloseStatus.NORMAL);
        }

        @Override
        public void close(CloseStatus closeStatus) throws IOException {
            if (isClosed) return;
            isClosed = true;
            if (closeStatus.equalsCode(CloseStatus.NORMAL)) {
                Void outcome = TestWebSocketConnection.this.serverHandler.afterConnectionClosedOk(
                        serverSessionAndState.getSession(),
                        serverSessionAndState.getState()
                );
                TestWebSocketConnection.this.serverSessionAndState.setClosedOk(outcome);
            } else {
                TestWebSocketConnection.this.serverHandler.afterConnectionClosedErroneously(
                        serverSessionAndState.getSession(), serverSessionAndState.getState(), closeStatus);
                TestWebSocketConnection.this.serverSessionAndState.setClosedError(closeStatus);
            }
            TestWebSocketConnection.this.clientSessionAndState.getSession().close(closeStatus);
        }
    }

    // TODO: share with ServerSession
    class ClientSession implements ITypedWebSocketSession<RunStreamInbound.Message> {
        private boolean isClosed = false;

        @Override
        public SessionId getId() {
            return TestWebSocketConnection.this.sessionId;
        }

        @Override
        public void sendMessage(RunStreamInbound.Message messageObject) throws IOException {
            if (isClosed) {
                System.out.println("tried to send on closed Client");
                throw new IOException("ClientSession is closed - cannot sendMessage");
            }
            TestWebSocketConnection.this.serverHandler.handleMessage(
                    serverSessionAndState.getSession(),
                    serverSessionAndState.getState(),
                    messageObject);
        }

        @Override
        public void closeOk() throws IOException {
            close(CloseStatus.NORMAL);
        }

        @Override
        public void close(CloseStatus closeStatus) throws IOException {
            if (isClosed) return;
            isClosed = true;

            // TODO: or more properly, we should be running the untyped handlers
            if (closeStatus.equalsCode(CloseStatus.NORMAL)) {
                OrError<String> outcome = TestWebSocketConnection.this.clientHandler.afterConnectionClosedOk(
                        clientSessionAndState.getSession(),
                        clientSessionAndState.getState()
                );
                TestWebSocketConnection.this.clientSessionAndState.setClosedOk(outcome);
            } else {
                TestWebSocketConnection.this.clientHandler.afterConnectionClosedErroneously(
                        clientSessionAndState.getSession(), clientSessionAndState.getState(), closeStatus);
                TestWebSocketConnection.this.clientSessionAndState.setClosedError(closeStatus);
            }

            TestWebSocketConnection.this.serverSessionAndState.getSession().close(closeStatus);
        }
    }

    public void establishConnection() {
        ITypedWebSocketSession<RunStreamOutbound.Message> serverSession = new ServerSession();

        ITypedWebSocketSession<RunStreamInbound.Message> clientSession = new ClientSession();

        RunnerSessionState serverState = serverHandler.handleConnectionEstablished(serverSession);
        RunnerClientWebSocketState clientState = clientHandler.handleConnectionEstablished(clientSession);

        serverSessionAndState = new TypedWebSocketSessionAndState<>(serverSession, serverState);
        clientSessionAndState = new TypedWebSocketSessionAndState<>(clientSession, clientState);
    }

    public TypedWebSocketSessionAndState<RunStreamInbound.Message, RunnerClientWebSocketState, OrError<String>> getClientSessionAndState() {
        return clientSessionAndState;
    }

    public void breakConnection() throws IOException {
        clientSessionAndState.getSession().close(CloseStatus.NO_CLOSE_FRAME);
        serverSessionAndState.getSession().close(CloseStatus.NO_CLOSE_FRAME);
    }

}