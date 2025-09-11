package uk.danielgooding.kokaplayground.compileandrun;

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
import java.util.concurrent.CompletableFuture;

@Controller
public class CompileAndRunWebSocketHandler
        implements TypedWebSocketHandler<
        CompileAndRunStream.Inbound.Message,
        CompileAndRunStream.Outbound.Message,
        CompileAndRunSessionState,
        Void> {

    private static final Logger logger = LoggerFactory.getLogger(CompileAndRunWebSocketHandler.class);

    @Autowired
    CompileServiceAPIClient compileServiceAPIClient;

    @Autowired
    ProxyingRunnerWebSocketClient proxyingRunnerWebSocketClient;

    @Autowired
    MeterRegistry meterRegistry;

    @Override
    public CompileAndRunSessionState handleConnectionEstablished(TypedWebSocketSession<CompileAndRunStream.Outbound.Message, Void> session) {
        Timer.Sample sessionSample = Timer.start(meterRegistry);
        return new CompileAndRunSessionState(sessionSample);
    }

    void compileAndRun(
            CompileAndRunStream.Inbound.CompileAndRun compileAndRun,
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, Void> session,
            CompileAndRunSessionState state) throws IOException {

        if (!state.isFirstRequest()) {
            session.sendMessage(new CompileAndRunStream.Outbound.AnotherRequestInProgress());
            session.closeErrorStatus("another request in progress", CloseStatus.POLICY_VIOLATION);
            return;
        }
        state.setReceivedRequest();

        session.sendMessage(new CompileAndRunStream.Outbound.StartingCompilation());

        logger.info("compiling: {}", session.getId());

        CompletableFuture<OrError<Void>> requestOutcomeFuture =
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
                            return proxyingRunnerWebSocketClient.execute(context).thenCompose(upstreamSession -> {
                                logger.info("began running: {}", session.getId());
                                try {
                                    state.onUpstreamConnectionEstablished(upstreamSession);
                                    state.sendUpstream(new RunStream.Inbound.Run(exeHandle));

                                    OrError<Void> result = OrError.ok(null);
                                    return CompletableFuture.completedFuture(result);

                                } catch (IOException e) {
                                    // failure to send upstream is a server error
                                    return CompletableFuture.failedFuture(e);
                                }
                            });
                        });

        requestOutcomeFuture.whenComplete((result, exn) -> {
            stopSessionTimer(state.getSessionTimerSample(), result, exn);

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
    }

    /// one of result/exn is not null
    void stopSessionTimer(Timer.Sample sessionTimerSample, OrError<Void> result, Throwable exn) {
        String outcome =
                exn != null ? "server-error" : switch (result) {
                    case Ok<Void> ignored -> "ok";
                    case Failed<?> clientError -> "client-error";
                };

        Timer timer = Timer.builder("compile_and_run.session")
                .publishPercentileHistogram()
                .tags("outcome", outcome)
                .register(meterRegistry);

        sessionTimerSample.stop(timer);
    }

    void handleStdin(
            CompileAndRunStream.Inbound.Stdin stdin,
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, Void> session,
            CompileAndRunSessionState state) throws IOException {
        state.sendUpstream(new RunStream.Inbound.Stdin(stdin.getContent()));
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, Void> session,
            CompileAndRunSessionState state,
            @NonNull CompileAndRunStream.Inbound.Message inbound) throws IOException {
        switch (inbound) {
            case CompileAndRunStream.Inbound.CompileAndRun compileAndRun ->
                    compileAndRun(compileAndRun, session, state);
            case CompileAndRunStream.Inbound.Stdin stdin -> handleStdin(stdin, session, state);
        }
    }

    @Override
    public Void afterConnectionClosedOk(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, Void> session,
            CompileAndRunSessionState state) throws IOException {

        state.closeUpstream();
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, Void> session,
            CompileAndRunSessionState state,
            CloseStatus status) throws IOException {

        state.closeUpstream();
    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, Void> session,
            CompileAndRunSessionState compileAndRunSessionState,
            Throwable exception) {
    }

    @Override
    public boolean isServer() {
        return true;
    }
}
