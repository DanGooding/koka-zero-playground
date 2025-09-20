package uk.danielgooding.kokaplayground.compileandrun;

import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;

/// a type alias for convenience
public class DownstreamSessionAndState
        extends TypedWebSocketSessionAndState<
        CompileAndRunStream.Outbound.Message, CompileAndRunSessionState, OrError<UserWorkStats>> {

    public DownstreamSessionAndState(
            TypedWebSocketSession<CompileAndRunStream.Outbound.Message, OrError<UserWorkStats>> session,
            CompileAndRunSessionState compileAndRunSessionState) {
        super(session, compileAndRunSessionState);
    }
}
