package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.lang.Nullable;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.util.ArrayList;
import java.util.List;

public class CompileAndRunSessionState {
    private @Nullable
    TypedWebSocketSessionAndState<RunStream.Inbound.Message, ?, Void>
            upstreamSessionAndState = null;
    private final List<RunStream.Inbound.Message> bufferedInbound;

    public CompileAndRunSessionState() {
        this.bufferedInbound = new ArrayList<>();
    }

    public void sendUpstream(RunStream.Inbound.Message message) throws Exception {
        if (upstreamSessionAndState == null) {
            bufferedInbound.add(message);
        } else {
            upstreamSessionAndState.getSession().sendMessage(message);
        }
    }

    public void onUpstreamConnectionEstablished(
            TypedWebSocketSessionAndState<
                    RunStream.Inbound.Message, ?, Void> upstreamSessionAndState
    ) throws Exception {
        this.upstreamSessionAndState = upstreamSessionAndState;

        for (RunStream.Inbound.Message message : bufferedInbound) {
            upstreamSessionAndState.getSession().sendMessage(message);
        }
        bufferedInbound.clear();
    }
}
