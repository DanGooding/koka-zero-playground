package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.RunStream;


@Service
public class RunnerClientWebSocketHandler
        implements TypedWebSocketHandler<RunStream.Outbound.Message, RunStream.Inbound.Message, RunnerClientWebSocketState, OrError<String>> {

    @Override
    public RunnerClientWebSocketState handleConnectionEstablished(ITypedWebSocketSession<RunStream.Inbound.Message> session) {
        // nothing
        return new RunnerClientWebSocketState();
    }

    @Override
    public void handleMessage(
            ITypedWebSocketSession<RunStream.Inbound.Message> session,
            RunnerClientWebSocketState state,
            @NonNull RunStream.Outbound.Message outbound
    ) {
        switch (outbound) {
            case RunStream.Outbound.AnotherRequestInProgress anotherRequestInProgress -> {
                state.setResult(OrError.error("not running: another run is in progress"));
            }
            case RunStream.Outbound.Starting starting -> {
            }
            case RunStream.Outbound.Stdout stdout -> {
                state.appendStdout(stdout.getContent());
            }
            case RunStream.Outbound.Done done -> {
                state.setResult(OrError.ok(null));
            }
            case RunStream.Outbound.Error error -> {
                state.setResult(OrError.error(String.format("error running: %s", error.getMessage())));
            }
            case RunStream.Outbound.Interrupted interrupted -> {
                state.setResult(OrError.error(String.format("run interrupted: %s", interrupted.getMessage())));
            }
        }
    }

    @Override
    public OrError<String> afterConnectionClosedOk(
            ITypedWebSocketSession<RunStream.Inbound.Message> session,
            RunnerClientWebSocketState state) {
        return state.getOutcome();
    }

    @Override
    public void afterConnectionClosedErroneously(
            ITypedWebSocketSession<RunStream.Inbound.Message> session,
            RunnerClientWebSocketState state,
            CloseStatus closeStatus) {
    }

    @Override
    public void handleTransportError(
            ITypedWebSocketSession<RunStream.Inbound.Message> session,
            RunnerClientWebSocketState state,
            Throwable exception) {

    }
}
