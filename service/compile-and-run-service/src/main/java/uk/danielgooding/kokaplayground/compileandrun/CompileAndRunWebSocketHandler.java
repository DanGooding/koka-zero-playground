package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;

@Controller
public class CompileAndRunWebSocketHandler
        implements TypedWebSocketHandler<
        CompileAndRunStream.Inbound.Message,
        CompileAndRunStream.Outbound.Message,
        CompileAndRunSessionState,
        Void> {

    @Autowired
    CompileServiceAPIClient compileServiceAPIClient;

    // TODO: will the websocket scope actually give us a per session instance?
    @Autowired
    ProxyingRunnerWebSocketClient proxyingRunnerWebSocketClient;

    @Override
    public CompileAndRunSessionState handleConnectionEstablished(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session) throws Exception {

        CompileAndRunSessionState state = new CompileAndRunSessionState();

        // TODO: perhaps we need to immediately await these futures?
        // otherwise we can't raise their exceptions in the right contexts?

        return state;
    }

    @Override
    public void handleMessage(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState state, @NonNull CompileAndRunStream.Inbound.Message inbound) throws Exception {
        switch (inbound) {
            case CompileAndRunStream.Inbound.CompileAndRun compileAndRun -> {
                OrError.thenComposeFuture(compileServiceAPIClient.compile(compileAndRun.getSourceCode()),
                        exeHandle -> {

                            ProxyingRunnerClientState context =
                                    new ProxyingRunnerClientState(session, state);
                            proxyingRunnerWebSocketClient.execute(context).thenCompose(upstreamSessionAndState -> {
                                // TODO: failed to deliver buffered to upstream - what to do?
                                state.onUpstreamConnectionEstablished(upstreamSessionAndState);
                                state.sendUpstream(new RunStream.Inbound.Run(exeHandle));


                                // TODO: on messages, propagate downstream
                                // don't really care about outcome
                            });

                        });
            }
            case CompileAndRunStream.Inbound.Stdin stdin ->
                    throw new UnsupportedOperationException("stdin not supported yet");
        }
    }

    @Override
    public Void afterConnectionClosedOk(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState compileAndRunSessionState) throws Exception {
        // TODO: propagate the close ?
        return null;
    }

    @Override
    public void afterConnectionClosedErroneously(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState compileAndRunSessionState, CloseStatus status) throws Exception {
        // TODO: propagate the close ?
    }

    @Override
    public void handleTransportError(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState compileAndRunSessionState, Throwable exception) throws Exception {

    }
}
