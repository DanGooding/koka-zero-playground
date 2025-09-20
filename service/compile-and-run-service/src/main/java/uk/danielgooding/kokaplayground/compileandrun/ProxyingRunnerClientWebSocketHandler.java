package uk.danielgooding.kokaplayground.compileandrun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.IStatelessTypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.io.IOException;

public class ProxyingRunnerClientWebSocketHandler
        implements IStatelessTypedWebSocketHandler<
        RunStream.Outbound.Message,
        RunStream.Inbound.Message,
        Void> {

    private final DownstreamSessionAndState downstreamSessionAndState;
    private static final Logger logger = LoggerFactory.getLogger(ProxyingRunnerClientWebSocketHandler.class);

    public ProxyingRunnerClientWebSocketHandler(DownstreamSessionAndState state) {
        this.downstreamSessionAndState = state;
    }

    @Override
    public void
    handleConnectionEstablished(TypedWebSocketSession<RunStream.Inbound.Message, Void> session) {
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<RunStream.Inbound.Message, Void> session,
            @NonNull RunStream.Outbound.Message outbound
    ) throws IOException {
        switch (outbound) {
            case RunStream.Outbound.AnotherRequestInProgress anotherRequestInProgress -> {
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.AnotherRequestInProgress());
            }
            case RunStream.Outbound.Starting starting -> {
                downstreamSessionAndState.getState().setState(CompileAndRunSessionState.StateTag.RUNNING);
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
                downstreamSessionAndState.getSession().closeOk(
                        OrError.ok(new UserWorkStats(done.getUserWorkDuration())));
            }
            case RunStream.Outbound.Error error -> {
                String message = String.format(
                        "error running: %s", error.getMessage());
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.Error(message));
                downstreamSessionAndState.getSession().closeOk(OrError.error(message));
            }
            case RunStream.Outbound.Interrupted interrupted -> {
                String message = String.format(
                        "run interrupted: %s", interrupted.getMessage());
                downstreamSessionAndState.getSession()
                        .sendMessage(new CompileAndRunStream.Outbound.Interrupted(message));
                downstreamSessionAndState.getSession().closeOk(OrError.error(message));
            }
        }
    }

    @Override
    public Void afterConnectionClosedOk(
            TypedWebSocketSession<RunStream.Inbound.Message, Void> session) throws IOException {

        // This will only trigger a close if we haven't already closed due to a message above.
        // Such a situation means that upstream closed the connection without sending us everything
        // we expected, so is an error.
        downstreamSessionAndState.getSession().closeErrorStatus(
                "upstream session closed ok but without completing request", CloseStatus.SERVER_ERROR);
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<RunStream.Inbound.Message, Void> session,
            CloseStatus closeStatus) throws IOException {

        downstreamSessionAndState.getSession().closeErrorStatus(
                String.format("websocket connection closed with error %s", this.getClass().getSimpleName()), closeStatus);
    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<RunStream.Inbound.Message, Void> session,
            Throwable exception) {
    }

    @Override
    public boolean isServer() {
        return false;
    }
}
