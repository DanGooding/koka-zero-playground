package uk.danielgooding.kokaplayground.compileandrun;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;

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
    public CompileAndRunSessionState handleConnectionEstablished(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session) throws Exception {
        return new CompileAndRunSessionState();
    }

    @Override
    public void handleMessage(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState state, @NonNull CompileAndRunStream.Inbound.Message inbound) throws Exception {
        switch (inbound) {
            case CompileAndRunStream.Inbound.CompileAndRun compileAndRun -> {

                if (!state.isFirstRequest()) {
                    session.sendMessage(new CompileAndRunStream.Outbound.AnotherRequestInProgress());
                    session.close(CloseStatus.POLICY_VIOLATION);
                    return;
                }
                state.setReceivedRequest();

                session.sendMessage(new CompileAndRunStream.Outbound.StartingCompilation());

                log.info(String.format("compiling: %s", session.getId()));
                OrError.thenComposeFuture(compileServiceAPIClient.compile(compileAndRun.getSourceCode()),
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
                                    return null;

                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        });
            }
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

        state.closeUpstream(status);
    }

    @Override
    public void handleTransportError(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState compileAndRunSessionState, Throwable exception) {
    }
}
