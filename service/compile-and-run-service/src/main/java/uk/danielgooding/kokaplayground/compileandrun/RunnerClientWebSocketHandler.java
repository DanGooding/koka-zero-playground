package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.protocol.RunStreamOutbound;
import uk.danielgooding.kokaplayground.protocol.RunStreamInbound;


@Service
public class RunnerClientWebSocketHandler
        implements TypedWebSocketHandler<RunStreamOutbound.Message, RunStreamInbound.Message, StringBuilder> {

    @Override
    public StringBuilder handleConnectionEstablished(TypedWebSocketSession<RunStreamInbound.Message> session) {
        // nothing
        return new StringBuilder();
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<RunStreamInbound.Message> session,
            StringBuilder stdoutBuilder,
            @NonNull RunStreamOutbound.Message outbound
    ) {
        // TODO: wrap handler in decorators? - not the exception one, perhaps the logging one?

        switch (outbound) {
            case RunStreamOutbound.AnotherRequestInProgress anotherRequestInProgress -> {
                // TODO: client error - propagate
            }
            case RunStreamOutbound.Starting starting -> {
            }
            case RunStreamOutbound.Stdout stdout -> {
                stdoutBuilder.append(stdout.getContent());
            }
            case RunStreamOutbound.Done done -> {
                // TODO: propagate outcome as request result
            }
            case RunStreamOutbound.Error error -> {
                // TODO: user error - propagate
            }
            case RunStreamOutbound.Interrupted interrupted -> {
                // TODO: outcome
            }
        }
    }

    @Override
    public void afterConnectionClosed(
            TypedWebSocketSession<RunStreamInbound.Message> session,
            StringBuilder stdout,
            CloseStatus status) throws Exception {

    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<RunStreamInbound.Message> session,
            StringBuilder stdout,
            Throwable exception) throws Exception {

    }
}
