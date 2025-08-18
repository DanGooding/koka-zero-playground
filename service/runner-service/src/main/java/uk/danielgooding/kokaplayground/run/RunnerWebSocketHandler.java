package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.RunStream;

@Controller
public class RunnerWebSocketHandler
        implements TypedWebSocketHandler<RunStream.Inbound.Message, RunStream.Outbound.Message, RunnerSessionState, Void> {

    @Autowired
    RunnerService runnerService;

    @Override
    public RunnerSessionState handleConnectionEstablished(ITypedWebSocketSession<RunStream.Outbound.Message> session) {
        return new RunnerSessionState();
    }

    @Override
    public void handleMessage(
            ITypedWebSocketSession<RunStream.Outbound.Message> session,
            RunnerSessionState sessionState,
            @NonNull RunStream.Inbound.Message inbound) throws Exception {
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
                                    session.close(CloseStatus.SERVER_ERROR);
                                } else {
                                    session.closeOk();
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
            ITypedWebSocketSession<RunStream.Outbound.Message> session,
            RunnerSessionState sessionState) {
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(
            ITypedWebSocketSession<RunStream.Outbound.Message> session,
            RunnerSessionState sessionState,
            CloseStatus status) {
        // nothing to cleanup
    }

    @Override
    public void handleTransportError(
            ITypedWebSocketSession<RunStream.Outbound.Message> session,
            RunnerSessionState sessionState, Throwable exception) {
        // not required to do anything (the client will find out about the close)
        // however we could abort the run if it isn't already
    }
}
