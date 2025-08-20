package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.lang.Nullable;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompileAndRunSessionState {
    /// upstreamSessionAndState will be populated once the connection is established
    private @Nullable
    TypedWebSocketSession<RunStream.Inbound.Message, Void>
            upstreamSessionAndState = null;

    /// proxied events are buffered until the upstream connection is create
    private @Nullable CloseStatus upstreamCloseStatus = null;
    private final List<RunStream.Inbound.Message> bufferedInbound;

    private boolean receivedRequest = false;

    public CompileAndRunSessionState() {
        this.bufferedInbound = new ArrayList<>();
    }

    public void sendUpstream(RunStream.Inbound.Message message) throws IOException {
        if (upstreamSessionAndState == null) {
            bufferedInbound.add(message);
        } else {
            upstreamSessionAndState.sendMessage(message);
        }
    }

    private static void closeUpstreamInternal(
            TypedWebSocketSession<RunStream.Inbound.Message, Void>
                    upstreamSessionAndState,
            CloseStatus closeStatus) throws IOException {
        if (closeStatus.equalsCode(CloseStatus.NORMAL)) {
            upstreamSessionAndState.closeOk(null);
        } else {
            upstreamSessionAndState.closeError(closeStatus);
        }
    }

    public void onUpstreamConnectionEstablished(
            TypedWebSocketSession<
                    RunStream.Inbound.Message, Void> upstreamSession
    ) throws IOException {
        this.upstreamSessionAndState = upstreamSession;

        if (upstreamCloseStatus != null) {
            // deliver the buffered close
            closeUpstreamInternal(upstreamSession, upstreamCloseStatus);
            return;
        }

        for (RunStream.Inbound.Message message : bufferedInbound) {
            upstreamSession.sendMessage(message);
        }
        bufferedInbound.clear();
    }


    public void closeUpstream(CloseStatus status) throws IOException {
        upstreamCloseStatus = status;
        if (upstreamSessionAndState != null) {
            // close unless not yet opened
            closeUpstreamInternal(upstreamSessionAndState, status);
        }
    }

    public boolean isFirstRequest() {
        return !receivedRequest;
    }

    public void setReceivedRequest() {
        receivedRequest = true;
    }
}
