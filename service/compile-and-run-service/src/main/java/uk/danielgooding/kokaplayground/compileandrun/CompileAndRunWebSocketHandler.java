package uk.danielgooding.kokaplayground.compileandrun;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.Failed;
import uk.danielgooding.kokaplayground.common.Ok;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.util.concurrent.CompletableFuture;

@Controller
public class CompileAndRunWebSocketHandler
        implements TypedWebSocketHandler<
        CompileAndRunStream.Inbound.Message,
        CompileAndRunStream.Outbound.Message,
        CompileAndRunSessionState,
        Void> {

    private static final Log log = LogFactory.getLog(CompileAndRunWebSocketHandler.class);
    @Autowired
    CompileServiceAPIClient compileServiceAPIClient;

    @Autowired
    ProxyingRunnerWebSocketClient proxyingRunnerWebSocketClient;

    @Override
    public CompileAndRunSessionState handleConnectionEstablished(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session) {
        return new CompileAndRunSessionState();
    }

    void compileAndRun(
            CompileAndRunStream.Inbound.CompileAndRun compileAndRun,
            ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session,
            CompileAndRunSessionState state) throws Exception {

        if (!state.isFirstRequest()) {
            session.sendMessage(new CompileAndRunStream.Outbound.AnotherRequestInProgress());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        state.setReceivedRequest();

        session.sendMessage(new CompileAndRunStream.Outbound.StartingCompilation());

        log.info(String.format("compiling: %s", session.getId()));

        CompletableFuture<OrError<Void>> requestOutcomeFuture =
                OrError.thenComposeFuture(
                        // failed is a server error, OrError.error is a client error
                        compileServiceAPIClient.compile(compileAndRun.getSourceCode()),
                        exeHandle -> {
                            log.info(String.format("compiled: %s", session.getId()));
                            try {
                                session.sendMessage(new CompileAndRunStream.Outbound.Running());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            ProxyingRunnerClientState context =
                                    new ProxyingRunnerClientState(session, state);
                            log.info(String.format("will request run: %s", session.getId()));
                            return proxyingRunnerWebSocketClient.execute(context).thenCompose(upstreamSessionAndState -> {
                                log.info(String.format("began running: %s", session.getId()));
                                try {
                                    state.onUpstreamConnectionEstablished(upstreamSessionAndState);
                                    state.sendUpstream(new RunStream.Inbound.Run(exeHandle));

                                    OrError<Void> result = OrError.ok(null);
                                    return CompletableFuture.completedFuture(result);

                                } catch (Exception e) {
                                    // failure to send upstream is a server error
                                    return CompletableFuture.failedFuture(e);
                                }
                            });
                        });

        requestOutcomeFuture.whenComplete((result, exn) -> {
            if (exn != null) {
                // server error

                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (Exception e) {
                    // okay to swallow - already closed
                    log.error("failed to close downstream after upstream error", e);
                }
            } else {
                switch (result) {
                    case Ok<Void> ignored -> {
                    }
                    case Failed<?> failed -> {
                        try {
                            session.sendMessage(new CompileAndRunStream.Outbound.Error(failed.getMessage()));
                            session.closeOk();
                        } catch (Exception e) {
                            log.error("failed to close downstream after client error", e);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void handleMessage(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState state, @NonNull CompileAndRunStream.Inbound.Message inbound) throws Exception {
        switch (inbound) {
            case CompileAndRunStream.Inbound.CompileAndRun compileAndRun ->
                    compileAndRun(compileAndRun, session, state);
            case CompileAndRunStream.Inbound.Stdin stdin ->
                    throw new UnsupportedOperationException("stdin not supported yet");
        }
    }

    @Override
    public Void afterConnectionClosedOk(
            ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session,
            CompileAndRunSessionState state) {

        state.closeUpstream(CloseStatus.GOING_AWAY);
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(
            ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session,
            CompileAndRunSessionState state,
            CloseStatus status) {

        // if we failed, it doesn't mean that upstream caused this
        state.closeUpstream(CloseStatus.GOING_AWAY);
    }

    @Override
    public void handleTransportError(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState compileAndRunSessionState, Throwable exception) {
    }
}
