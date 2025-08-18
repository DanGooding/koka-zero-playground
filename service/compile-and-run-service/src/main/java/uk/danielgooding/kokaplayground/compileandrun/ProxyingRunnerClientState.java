package uk.danielgooding.kokaplayground.compileandrun;

import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;

/// a type alias for convenience
public class ProxyingRunnerClientState extends TypedWebSocketSessionAndState<CompileAndRunStream.Outbound.Message, CompileAndRunSessionState, Void> {

    public ProxyingRunnerClientState(ITypedWebSocketSession<CompileAndRunStream.Outbound.Message> session, CompileAndRunSessionState compileAndRunSessionState) {
        super(session, compileAndRunSessionState);
    }
}
