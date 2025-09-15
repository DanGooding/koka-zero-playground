package uk.danielgooding.kokaplayground.run;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Timer userWorkTimer;

    private static final Logger logger = LoggerFactory.getLogger(RunnerWebSocketHandler.class);

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
        userWorkTimer = Timer.builder("request.session.user_work")
                .publishPercentileHistogram()
                .register(meterRegistry);
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
                        .thenApply(error -> {
                            try {
                                session.sendMessage(
                                        switch (error) {
                                            case Ok<RunStats> ok -> new RunStream.Outbound.Done();
                                            case Failed<?> failed -> {
                                                String message = failed.getMessage();
                                                if (message.length() > maxErrorBytes) {
                                                    message = message.substring(0, maxErrorBytes) + "...";
                                                }
                                                yield new RunStream.Outbound.Error(message);
                                            }
                                        });
                                return error;
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

    public void stopSessionTimer(RunnerSessionState state, OrCancelled<OrError<RunStats>> result, Throwable exn) {
        String outcome =
                exn != null ? "server-error" : switch (result) {
                    case OrCancelled.Cancelled<?> cancelled -> "ok";
                    case OrCancelled.Ok<OrError<RunStats>> orError -> switch (orError.getResult()) {
                        case Ok<RunStats> ignored -> "ok";
                        case Failed<?> clientError -> "client-error";
                    };
                };

        Timer timer = timerProvider.withTags("outcome", outcome);
        state.getSessionSample().stop(timer);

        if (exn == null && result instanceof OrCancelled.Ok<OrError<RunStats>> notCancelled) {
            if (notCancelled.getResult() instanceof Ok<RunStats> runStatsOk) {
                RunStats runStats = runStatsOk.getValue();
                userWorkTimer.record(runStats.userWorkDuration());
            }
        }
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
