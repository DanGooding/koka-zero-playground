package uk.danielgooding.kokaplayground.run;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import java.util.List;

@Controller
public class RunnerWebSocketHandler
        implements TypedWebSocketHandler<
        RunStream.Inbound.Message,
        RunStream.Outbound.Message,
        RunnerSessionState,
        RunnerSessionState.StateTag,
        Void> {

    private final RunnerService runnerService;
    private final MeterRegistry meterRegistry;
    private final int maxBufferedStdinItems;
    private final int maxErrorBytes;
    private final Meter.MeterProvider<Timer> timerProvider;

    public RunnerWebSocketHandler(
            @Autowired RunnerService runnerService,
            @Autowired MeterRegistry meterRegistry,
            @Value("${runner.max-buffered-stdin-items}") int maxBufferedStdinItems,
            @Value("${runner.max-stderr-bytes}") int maxErrorBytes) {
        this.runnerService = runnerService;
        this.meterRegistry = meterRegistry;
        this.maxBufferedStdinItems = maxBufferedStdinItems;
        this.maxErrorBytes = maxErrorBytes;

        timerProvider = Timer.builder("request.session")
                .publishPercentileHistogram()
                .withRegistry(meterRegistry);
    }

    @Override
    public RunnerSessionState handleConnectionEstablished(TypedWebSocketSession<RunStream.Outbound.Message, Void> session) {
        Timer.Sample sessionSample = Timer.start(meterRegistry);
        return new RunnerSessionState(maxBufferedStdinItems, sessionSample);
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

        Callback<Void> onStart = (ignored) -> {
            sessionState.setState(RunnerSessionState.StateTag.RUNNING);
            session.sendMessage(new RunStream.Outbound.Starting());
        };

        Callback<String> onStdout = (chunk) -> {
            session.sendMessage(new RunStream.Outbound.Stdout(chunk));
        };

        sessionState.setState(RunnerSessionState.StateTag.AWAITING_RUN);
        sessionState.setRunning(
                runnerService.runStreamingStdinAndStdout(
                                run.getExeHandle(),
                                sessionState.getStdinBuffer(),
                                onStart,
                                onStdout)
                        .thenApply(result -> {
                            try {
                                session.sendMessage(
                                        switch (result) {
                                            case OrInterrupted.Interrupted<?> interrupted ->
                                                    new RunStream.Outbound.Interrupted("runtime limit exceeded");

                                            case OrInterrupted.Ok<OrError<Void>> resultNotInterrupted ->
                                                    switch (resultNotInterrupted.getResult()) {
                                                        case Ok<Void> ok -> new RunStream.Outbound.Done();
                                                        case Failed<?> failed -> {
                                                            String message = failed.getMessage();
                                                            if (message.length() > maxErrorBytes) {
                                                                message = message.substring(0, maxErrorBytes) + "...";
                                                            }
                                                            yield new RunStream.Outbound.Error(message);
                                                        }
                                                    };
                                        });
                                return result;
                            } catch (IOException e) {
                                // next block will handle
                                throw new UncheckedIOException(e);
                            }
                        })
                        .whenComplete((result, exn) -> {
                            stopSessionTimer(sessionState, result, exn);
                            sessionState.setState(RunnerSessionState.StateTag.COMPLETE);

                            try {
                                if (exn != null) {
                                    session.closeExn("failure in Runner service", exn);
                                } else {
                                    session.closeOk(null);
                                }
                            } catch (IOException e) {
                                // okay to swallow - already failing due to original exn.
                            }
                        }));
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

        sessionState.cancelCurrentRun();
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState,
            CloseStatus status) {

        sessionState.cancelCurrentRun();
    }

    public void stopSessionTimer(
            RunnerSessionState state,
            OrCancelled<OrInterrupted<OrError<Void>>> result,
            Throwable exn) {
        String outcome =
                exn != null ? "server-error" : switch (result) {
                    case OrCancelled.Cancelled<?> cancelled -> "ok";
                    case OrCancelled.Ok<OrInterrupted<OrError<Void>>> orInterrupted ->
                            switch (orInterrupted.getResult()) {
                                case OrInterrupted.Interrupted<?> interrupted -> "client-error";
                                case OrInterrupted.Ok<OrError<Void>> orError -> switch (orError.getResult()) {
                                    case Ok<Void> ignored -> "ok";
                                    case Failed<?> clientError -> "client-error";
                                };
                            };

                };

        Timer timer = timerProvider.withTags("outcome", outcome);
        state.getSessionSample().stop(timer);
    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<RunStream.Outbound.Message, Void> session,
            RunnerSessionState sessionState, Throwable exception) {
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public Iterable<RunnerSessionState.StateTag> allSessionStateTags() {
        return List.of(RunnerSessionState.StateTag.values());
    }
}
