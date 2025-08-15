package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.*;

import java.io.IOException;
import java.util.HashSet;

@Controller
public class RunnerWebSocketHandler implements TypedWebSocketHandler<RunStreamInbound.Message, RunStreamOutbound.Message> {
    private final HashSet<SessionId> runningForIds;

    @Autowired
    RunnerService runnerService;

    public RunnerWebSocketHandler() {
        runningForIds = new HashSet<>();
    }

    public void handleConnectionEstablished(TypedWebSocketSession<RunStreamOutbound.Message> session) {
    }

    public void handleMessage(TypedWebSocketSession<RunStreamOutbound.Message> session, @NonNull RunStreamInbound.Message inbound) throws Exception {
        switch (inbound) {
            case RunStreamInbound.Run run -> {
                if (runningForIds.contains(session.getId())) {
                    session.sendMessage(new RunStreamOutbound.AnotherRequestInProgress());
                    return;
                }

                runningForIds.add(session.getId());

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
                            runningForIds.remove(session.getId());
                            if (exn != null) {
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
                        });
            }
            case RunStreamInbound.Stdin stdin -> {
                throw new UnsupportedOperationException("Stdin not yet supported");
            }
        }
    }

    public void afterConnectionClosed(TypedWebSocketSession<RunStreamOutbound.Message> session, CloseStatus status) {
        runningForIds.remove(session.getId());
        // TODO: cleanup - e.g. cancel the run
    }

    public void handleTransportError(TypedWebSocketSession<RunStreamOutbound.Message> session, Throwable exception) {
        // TODO: handle transport error
    }
}
