package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.RunStreamOutbound;
import uk.danielgooding.kokaplayground.protocol.RunStreamInbound;


@Service
public class RunnerClientWebSocketHandler
        implements TypedWebSocketHandler<RunStreamOutbound.Message, RunStreamInbound.Message, RunnerClientWebSocketState, OrError<String>> {

    @Override
    public RunnerClientWebSocketState handleConnectionEstablished(ITypedWebSocketSession<RunStreamInbound.Message> session) {
        // nothing
        return new RunnerClientWebSocketState();
    }

    @Override
    public void handleMessage(
            ITypedWebSocketSession<RunStreamInbound.Message> session,
            RunnerClientWebSocketState state,
            @NonNull RunStreamOutbound.Message outbound
    ) {
        switch (outbound) {
            case RunStreamOutbound.AnotherRequestInProgress anotherRequestInProgress -> {
                state.setResult(OrError.error("not running: another run is in progress"));
            }
            case RunStreamOutbound.Starting starting -> {
            }
            case RunStreamOutbound.Stdout stdout -> {
                state.appendStdout(stdout.getContent());
            }
            case RunStreamOutbound.Done done -> {
                state.setResult(OrError.ok(null));
            }
            case RunStreamOutbound.Error error -> {
                state.setResult(OrError.error(String.format("error running: %s", error.getMessage())));
            }
            case RunStreamOutbound.Interrupted interrupted -> {
                state.setResult(OrError.error(String.format("run interrupted: %s", interrupted.getMessage())));
            }
        }
    }

    @Override
    public OrError<String> afterConnectionClosedOk(
            ITypedWebSocketSession<RunStreamInbound.Message> session,
            RunnerClientWebSocketState state) {
        // TODO: if closed erroneously, don't want to use outcome
        return state.getOutcome();
    }

    @Override
    public void afterConnectionClosedErroneously(
            ITypedWebSocketSession<RunStreamInbound.Message> session,
            RunnerClientWebSocketState state,
            CloseStatus closeStatus) {
    }

    @Override
    public void handleTransportError(
            ITypedWebSocketSession<RunStreamInbound.Message> session,
            RunnerClientWebSocketState state,
            Throwable exception) {

    }
}
