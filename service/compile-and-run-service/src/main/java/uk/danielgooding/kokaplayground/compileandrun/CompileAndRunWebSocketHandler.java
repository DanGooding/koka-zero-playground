package uk.danielgooding.kokaplayground.compileandrun;

import com.netflix.concurrency.limits.Limiter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.Failed;
import uk.danielgooding.kokaplayground.common.Ok;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
public class CompileAndRunWebSocketHandler
        implements TypedWebSocketHandler<
        CompileAndRunStream.Inbound.Message,
        CompileAndRunStream.Outbound.Message,
        CompileAndRunSessionState,
        CompileAndRunSessionState.StateTag,
        OrError<UserWorkStats>> {

    private static final Logger logger = LoggerFactory.getLogger(CompileAndRunWebSocketHandler.class);

    private final CompileServiceAPIClient compileServiceAPIClient;
    private final ProxyingRunnerWebSocketClient proxyingRunnerWebSocketClient;
    private final MeterRegistry meterRegistry;
    private final Meter.MeterProvider<Timer> timerProvider;
    private final Timer nonUserWorkTimer;

    public CompileAndRunWebSocketHandler(
            @Autowired CompileServiceAPIClient compileServiceAPIClient,
            @Autowired ProxyingRunnerWebSocketClient proxyingRunnerWebSocketClient,
            @Autowired MeterRegistry meterRegistry) {

        this.compileServiceAPIClient = compileServiceAPIClient;
        this.proxyingRunnerWebSocketClient = proxyingRunnerWebSocketClient;
        this.meterRegistry = meterRegistry;

        timerProvider = Timer.builder("request.session")
                .publishPercentileHistogram()
                .withRegistry(meterRegistry);

        nonUserWorkTimer = Timer.builder("request.session.non_user_work")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @Override
    public CompileAndRunSessionState handleConnectionEstablished(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session) {

        return new CompileAndRunSessionState(meterRegistry);
    }

    void compileAndRun(
            CompileAndRunStream.Inbound.CompileAndRun compileAndRun,
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session,
            CompileAndRunSessionState state) throws IOException {

        if (state.getStateTag() != CompileAndRunSessionState.StateTag.AWAITING_REQUEST) {
            session.sendMessage(new CompileAndRunStream.Outbound.AnotherRequestInProgress());
            session.closeErrorStatus("another request in progress", CloseStatus.POLICY_VIOLATION);
            return;
        }
        state.setState(CompileAndRunSessionState.StateTag.COMPILING);
        logger.info("compiling: {}", session.getId());
        session.sendMessage(new CompileAndRunStream.Outbound.StartingCompilation());

        CompletableFuture<OrError<Void>> connectToUpstreamOutcome =
                OrError.thenComposeFuture(
                        // failed is a server error, OrError.error is a client error
                        compileServiceAPIClient.compile(compileAndRun.getSourceCode()),
                        exeHandle -> {
                            logger.info("compiled: {}", session.getId());
                            try {
                                session.sendMessage(new CompileAndRunStream.Outbound.StartingRun());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            ProxyingRunnerClientState context =
                                    new ProxyingRunnerClientState(session, state);
                            logger.info("will request run: {}", session.getId());
                            state.setState(CompileAndRunSessionState.StateTag.CONNECTING_TO_RUNNER);

                            return proxyingRunnerWebSocketClient.execute(context).thenCompose(
                                    upstreamSession -> {
                                        logger.info("began running: {}", session.getId());
                                        state.setState(CompileAndRunSessionState.StateTag.AWAITING_RUN);
                                        try {
                                            state.onUpstreamConnectionEstablished(upstreamSession);
                                            state.sendUpstream(new RunStream.Inbound.Run(exeHandle));

                                            // We intentionally don't wait for the upstream request to complete here.
                                            // The proxying client will handle the outcome of the session once it's setup.
                                            OrError<Void> result = OrError.ok(null);
                                            return CompletableFuture.completedFuture(result);

                                        } catch (IOException e) {
                                            // failure to send upstream is a server error
                                            return CompletableFuture.failedFuture(e);
                                        }
                                    });
                        });

        // Error handling specifically for compilation, and sending the initial request to the runner.
        // Once we're connected to the runner, all subsequent handling is done by the proxying client.
        connectToUpstreamOutcome.whenComplete((result, exn) -> {
            if (exn != null) {
                // server error
                try {
                    session.closeExn("failure in CompileAndRun handler", exn);
                } catch (Exception e) {
                    // okay to swallow - already closed
                    logger.error("failed to close downstream after upstream error", e);
                }
            } else {
                switch (result) {
                    case Ok<Void> ignored -> {
                    }
                    case Failed<?> failed -> {
                        try {
                            session.sendMessage(new CompileAndRunStream.Outbound.Error(failed.getMessage()));
                            session.closeOk(null);
                        } catch (Exception e) {
                            logger.error("failed to close downstream after client error", e);
                        }
                    }
                }
            }
        });

        // Overall outcome of this session - when the run completes or fails.
        session.getOutcomeFuture().whenComplete((result, exn) -> {
            stopSessionTimer(session, state, result, exn);
            state.setState(CompileAndRunSessionState.StateTag.COMPLETE);
        });
    }

    void stopSessionTimer(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session,
            CompileAndRunSessionState state,
            OrError<UserWorkStats> result,
            Throwable exn
    ) {
        String outcome = exn != null ? "server-error" : "ok";

        Timer timer = timerProvider.withTags("outcome", outcome);
        state.getSessionTimerSample().stop(timer);

        Duration nonUserWorkDuration = null;
        if (result instanceof Ok<UserWorkStats> userWorkStatsOk) {
            UserWorkStats userWorkStats = userWorkStatsOk.getValue();
            if (userWorkStats != null) {
                Duration duration = state.getDuration();
                nonUserWorkDuration = duration.minus(userWorkStats.getUserWorkDuration());

                nonUserWorkTimer.record(nonUserWorkDuration);
            }
        }

        Object maybeListener = session.getAttributes().get(ConcurrencyLimitingInterceptor.listenerAttributeName);
        if (maybeListener instanceof Limiter.Listener listener) {

            if (exn != null) {
                listener.onIgnore();
            } else {
                switch (result) {
                    // TODO: need to subtract the user duration :(
                    case Ok<?> ok -> listener.onSuccess();
                    case Failed<?> failed -> listener.onIgnore();
                }
            }
        }
    }

    void handleStdin(
            CompileAndRunStream.Inbound.Stdin stdin,
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session,
            CompileAndRunSessionState state) throws IOException {
        state.sendUpstream(new RunStream.Inbound.Stdin(stdin.getContent()));
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session,
            CompileAndRunSessionState state,
            @NonNull CompileAndRunStream.Inbound.Message inbound) throws IOException {
        switch (inbound) {
            case CompileAndRunStream.Inbound.CompileAndRun compileAndRun ->
                    compileAndRun(compileAndRun, session, state);
            case CompileAndRunStream.Inbound.Stdin stdin -> handleStdin(stdin, session, state);
        }
    }

    @Override
    public OrError<UserWorkStats> afterConnectionClosedOk(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session,
            CompileAndRunSessionState state) throws IOException {

        state.closeUpstream();
        return OrError.ok(null);
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session,
            CompileAndRunSessionState state,
            CloseStatus status) throws IOException {

        state.closeUpstream();
    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session,
            CompileAndRunSessionState compileAndRunSessionState,
            Throwable exception) {
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public Iterable<CompileAndRunSessionState.StateTag> allSessionStateTags() {
        return List.of(CompileAndRunSessionState.StateTag.values());
    }
}
