package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.io.IOException;

@Controller
public class RunnerWebSocketHandler
        implements TypedWebSocketHandler<RunStream.Inbound.Message, RunStream.Outbound.Message, RunnerSessionState, Void> {

    @Autowired
    RunnerService runnerService;

    @Override
    public RunnerSessionState handleConnectionEstablished(TypedWebSocketSession<RunStream.Outbound.Message, Void> session) {
        return new RunnerSessionState();
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState,
            @NonNull RunStream.Inbound.Message inbound) throws IOException {
        switch (inbound) {
            case RunStream.Inbound.Run run -> {
                if (sessionState.isRunning()) {
                    session.sendMessage(new RunStream.Outbound.AnotherRequestInProgress());
                    return;
                }

                sessionState.setRunning(true);

                Callback<Void> onStart = (ignored) -> {
                    session.sendMessage(new RunStream.Outbound.Starting());
                };

                Callback<String> onStdout = (chunk) -> {
                    session.sendMessage(new RunStream.Outbound.Stdout(chunk));
                };

                runnerService.runWithoutStdinStreamingStdout(
                                run.getExeHandle(),
                                onStart,
                                onStdout)
                        // TODO: yuck, - clean this up
                        .whenComplete((error, exn) -> {
                            sessionState.setRunning(false);

                            if (exn == null) {
                                try {
                                    session.sendMessage(
                                            switch (error) {
                                                case Ok<Void> ok -> new RunStream.Outbound.Done();
                                                case Failed<?> failed ->
                                                        new RunStream.Outbound.Error(failed.getMessage());
                                            });
                                } catch (Exception e) {
                                    // okay to swallow - already failing due to original exn.
                                }
                            }
                        }).whenComplete((error, exn) -> {
                            try {
                                if (exn != null) {
                                    session.closeError(CloseStatus.SERVER_ERROR);
                                } else {
                                    session.closeOk(null);
                                }
                            } catch (Exception e) {
                                // okay to swallow - already failing due to original exn.
                            }
                        });
            }
            case RunStream.Inbound.Stdin stdin -> {
                throw new UnsupportedOperationException("Stdin not yet supported");
            }
        }
    }

    @Override
    public Void afterConnectionClosedOk(
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState) {
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState,
            CloseStatus status) {
        // nothing to cleanup
    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState, Throwable exception) {
    }
}
