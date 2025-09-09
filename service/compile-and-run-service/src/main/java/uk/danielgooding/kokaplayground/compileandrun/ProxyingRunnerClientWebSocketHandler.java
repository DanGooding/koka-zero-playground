package uk.danielgooding.kokaplayground.compileandrun;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.io.IOException;


public class ProxyingRunnerClientWebSocketHandler
        implements TypedWebSocketHandler<
        RunStream.Outbound.Message,
        RunStream.Inbound.Message,
        Void,
        Void> {

    private final ProxyingRunnerClientState downstreamSessionAndState;
    private static final Log log = LogFactory.getLog(ProxyingRunnerClientWebSocketHandler.class);

    public ProxyingRunnerClientWebSocketHandler(ProxyingRunnerClientState state) {
        this.downstreamSessionAndState = state;
    }

    @Override
    public Void
    handleConnectionEstablished(TypedWebSocketSession<RunStream.Inbound.Message, Void> session) {
        return null;
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<RunStream.Inbound.Message, Void> session,
            Void ignored,
            @NonNull RunStream.Outbound.Message outbound
    ) throws IOException {
        switch (outbound) {
            case RunStream.Outbound.AnotherRequestInProgress anotherRequestInProgress -> {
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.AnotherRequestInProgress());
            }
            case RunStream.Outbound.Starting starting -> {
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.Running());
            }
            case RunStream.Outbound.Stdout stdout -> {
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.Stdout(stdout.getContent()));
            }
            case RunStream.Outbound.Done done -> {
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.Done());
            }
            case RunStream.Outbound.Error error -> {
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.Error(String.format(
                                "error running: %s", error.getMessage())));
            }
            case RunStream.Outbound.Interrupted interrupted -> {
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.Interrupted(String.format(
                                "run interrupted: %s", interrupted.getMessage())));
            }
        }
    }

    @Override
    public Void afterConnectionClosedOk(
            TypedWebSocketSession<RunStream.Inbound.Message, Void> session, Void ignored) throws IOException {

        downstreamSessionAndState.getSession().closeOk(null);
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<RunStream.Inbound.Message, Void> session,
            Void ignored,
            CloseStatus closeStatus) throws IOException {

        downstreamSessionAndState.getSession().closeErrorStatus(
                String.format("websocket connection closed with error %s", this.getClass()), closeStatus);
    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<RunStream.Inbound.Message, Void> session,
            Void ignored,
            Throwable exception) {
    }

    @Override
    public boolean isServer() {
        return false;
    }
}
