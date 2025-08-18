package uk.danielgooding.kokaplayground.compileandrun;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;


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
    handleConnectionEstablished(ITypedWebSocketSession<RunStream.Inbound.Message> session) {
        return null;
    }

    @Override
    public void handleMessage(
            ITypedWebSocketSession<RunStream.Inbound.Message> session,
            Void ignored,
            @NonNull RunStream.Outbound.Message outbound
    ) throws Exception {
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
            ITypedWebSocketSession<RunStream.Inbound.Message> session, Void ignored) {

        downstreamSessionAndState.setClosedOk(null);
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(
            ITypedWebSocketSession<RunStream.Inbound.Message> session,
            Void ignored,
            CloseStatus closeStatus) {

        downstreamSessionAndState.setClosedError(closeStatus);
    }

    @Override
    public void handleTransportError(
            ITypedWebSocketSession<RunStream.Inbound.Message> session,
            Void ignored,
            Throwable exception) {

        log.error("ProxyingRunnerClient: transport error", exception);
    }
}
