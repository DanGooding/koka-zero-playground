package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.protocol.RunStreamInbound;
import uk.danielgooding.kokaplayground.protocol.RunStreamOutbound;

import java.io.IOException;

@Controller
public class RunnerWebSocketHandler
        implements TypedWebSocketHandler<RunStreamInbound.Message, RunStreamOutbound.Message, RunnerSessionState> {

    @Autowired
    RunnerService runnerService;

    @Override
    public RunnerSessionState handleConnectionEstablished(TypedWebSocketSession<RunStreamOutbound.Message> session) {
        return new RunnerSessionState();
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<RunStreamOutbound.Message> session,
            RunnerSessionState sessionState,
            @NonNull RunStreamInbound.Message inbound) throws Exception {
        switch (inbound) {
            case RunStreamInbound.Run run -> {
                if (sessionState.isRunning()) {
                    session.sendMessage(new RunStreamOutbound.AnotherRequestInProgress());
                    return;
                }

                sessionState.setRunning(true);

                Callback<Void> onStart = (ignored) -> {
                    session.sendMessage(new RunStreamOutbound.Starting());
                };

                Callback<String> onStdout = (chunk) -> {
                    session.sendMessage(new RunStreamOutbound.Stdout(chunk));
                };

                runnerService.runWithoutStdinStreamingStdout(
                                run.getExeHandle(),
                                onStart,
                                onStdout)
                        .whenComplete((error, exn) -> {
                            sessionState.setRunning(false);

                            if (exn == null) {
                                try {
                                    session.sendMessage(
                                            switch (error) {
                                                case Ok<Void> ok -> new RunStreamOutbound.Done();
                                                case Failed<?> failed ->
                                                        new RunStreamOutbound.Error(failed.getMessage());
                                            });
                                } catch (IOException e) {
                                    // okay to swallow - already failing due to original exn.
                                }
                            }
                        }).whenComplete((error, exn) -> {
                            try {
                                session.close();
                            } catch (IOException e) {
                                // okay to swallow - already failing due to original exn.
                            }
                        });
            }
            case RunStreamInbound.Stdin stdin -> {
                throw new UnsupportedOperationException("Stdin not yet supported");
            }
        }
    }

    @Override
    public void afterConnectionClosed(
            TypedWebSocketSession<RunStreamOutbound.Message> session,
            RunnerSessionState sessionState,
            CloseStatus status) {
        sessionState.setRunning(false);
    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<RunStreamOutbound.Message> session,
            RunnerSessionState sessionState, Throwable exception) {
        // not required to do anything (the client will find out about the close)
        // however we could abort the run if it isn't already
    }
}
