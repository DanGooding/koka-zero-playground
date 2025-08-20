package uk.danielgooding.kokaplayground.compileandrun;

import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;

/// a type alias for convenience
public class ProxyingRunnerClientState extends TypedWebSocketSessionAndState<CompileAndRunStream.Outbound.Message, CompileAndRunSessionState, Void> {

    public ProxyingRunnerClientState(TypedWebSocketSession<CompileAndRunStream.Outbound.Message, Void> session, CompileAndRunSessionState compileAndRunSessionState) {
        super(session, compileAndRunSessionState);
    }
}
