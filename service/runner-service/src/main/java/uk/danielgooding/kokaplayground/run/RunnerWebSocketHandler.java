package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.io.IOException;
import java.io.UncheckedIOException;

@Controller
public class RunnerWebSocketHandler
        implements TypedWebSocketHandler<RunStream.Inbound.Message, RunStream.Outbound.Message, RunnerSessionState, Void> {

    @Autowired
    RunnerService runnerService;

    @Value("${runner.max-buffered-stdin-items}")
    int maxBufferedStdinItems;

    @Override
    public RunnerSessionState handleConnectionEstablished(TypedWebSocketSession<RunStream.Outbound.Message, Void> session) {
        return new RunnerSessionState(maxBufferedStdinItems);
    }

    public void handleRunMessage(
            RunStream.Inbound.Run run,
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState
    ) throws IOException {
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


        runnerService.runStreamingStdinAndStdout(
                        run.getExeHandle(),
                        sessionState.getStdinBuffer(),
                        onStart,
                        onStdout)
                .thenAccept(error -> {
                    try {
                        session.sendMessage(
                                switch (error) {
                                    case Ok<Void> ok -> new RunStream.Outbound.Done();
                                    case Failed<?> failed -> new RunStream.Outbound.Error(failed.getMessage());
                                });
                    } catch (IOException e) {
                        // next block will handle
                        throw new UncheckedIOException(e);
                    }
                })
                .whenComplete((error, exn) -> {
                    try {
                        if (exn != null) {
                            session.closeError(CloseStatus.SERVER_ERROR);
                        } else {
                            session.closeOk(null);
                        }
                    } catch (IOException e) {
                        // okay to swallow - already failing due to original exn.
                    } finally {
                        sessionState.setRunning(false);
                    }
                });
    }

    public void handleStdin(
            RunStream.Inbound.Stdin stdin,
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState
    ) {
        sessionState.bufferOrDropStdin(stdin.getContent());
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState,
            @NonNull RunStream.Inbound.Message inbound) throws IOException {
        switch (inbound) {
            case RunStream.Inbound.Run run -> {
                handleRunMessage(run, session, sessionState);
            }
            case RunStream.Inbound.Stdin stdin -> {
                handleStdin(stdin, session, sessionState);
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
