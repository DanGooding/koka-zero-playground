package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.lang.Nullable;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.util.ArrayList;
import java.util.List;

public class CompileAndRunSessionState {
    /// upstreamSessionAndState will be populated once the connection is established
    private @Nullable
    TypedWebSocketSessionAndState<RunStream.Inbound.Message, ?, Void>
            upstreamSessionAndState = null;

    /// proxied events are buffered until the upstream connection is create
    private @Nullable CloseStatus upstreamCloseStatus = null;
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

    private static void closeUpstreamInternal(
            TypedWebSocketSessionAndState<RunStream.Inbound.Message, ?, Void>
                    upstreamSessionAndState,
            CloseStatus closeStatus) {
        if (closeStatus.equalsCode(CloseStatus.NORMAL)) {
            upstreamSessionAndState.setClosedOk(null);
        } else {
            upstreamSessionAndState.setClosedError(closeStatus);
        }
    }

    public void onUpstreamConnectionEstablished(
            TypedWebSocketSessionAndState<
                    RunStream.Inbound.Message, ?, Void> upstreamSessionAndState
    ) throws Exception {
        this.upstreamSessionAndState = upstreamSessionAndState;

        if (upstreamCloseStatus != null) {
            // deliver the buffered close
            closeUpstreamInternal(upstreamSessionAndState, upstreamCloseStatus);
            return;
        }

        for (RunStream.Inbound.Message message : bufferedInbound) {
            upstreamSessionAndState.getSession().sendMessage(message);
        }
        bufferedInbound.clear();
    }


    public void closeUpstream(CloseStatus status) {
        upstreamCloseStatus = status;
        if (upstreamSessionAndState != null) {
            // close unless not yet opened
            closeUpstreamInternal(upstreamSessionAndState, status);
        }
    }
}
