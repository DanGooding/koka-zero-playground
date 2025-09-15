package uk.danielgooding.kokaplayground.compileandrun;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.lang.Nullable;
import uk.danielgooding.kokaplayground.common.websocket.ISessionState;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CompileAndRunSessionState
        implements ISessionState<CompileAndRunSessionState.StateTag> {
    /// upstreamSessionAndState will be populated once the connection is established
    private @Nullable
    TypedWebSocketSession<RunStream.Inbound.Message, Void>
            upstreamSessionAndState = null;

    /// proxied events are buffered until the upstream connection is create
    private @Nullable boolean bufferedCloseUpstream = false;
    private final List<RunStream.Inbound.Message> bufferedInbound;

    private StateTag state = StateTag.AWAITING_REQUEST;

    private final Timer.Sample sessionTimerSample;
    private final long startTime;

    public enum StateTag {
        AWAITING_REQUEST,
        COMPILING,
        CONNECTING_TO_RUNNER,
        AWAITING_RUN,
        RUNNING,
        COMPLETE
    }

    public CompileAndRunSessionState(MeterRegistry meterRegistry) {
        this.bufferedInbound = new ArrayList<>();
        this.sessionTimerSample = Timer.start(meterRegistry);
        this.startTime = System.nanoTime();
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
                    upstreamSessionAndState) throws IOException {

        // if we failed, it doesn't mean that upstream caused this
        upstreamSessionAndState.closeGoingAway(null);
    }

    public void onUpstreamConnectionEstablished(
            TypedWebSocketSession<
                    RunStream.Inbound.Message, Void> upstreamSession
    ) throws IOException {
        this.upstreamSessionAndState = upstreamSession;

        if (bufferedCloseUpstream) {
            // deliver the buffered close
            closeUpstreamInternal(upstreamSession);
            return;
        }

        for (RunStream.Inbound.Message message : bufferedInbound) {
            upstreamSession.sendMessage(message);
        }
        bufferedInbound.clear();
    }


    public void closeUpstream() throws IOException {
        if (upstreamSessionAndState != null) {
            // close unless not yet opened
            closeUpstreamInternal(upstreamSessionAndState);
        } else {
            bufferedCloseUpstream = true;
        }
    }

    public Timer.Sample getSessionTimerSample() {
        return sessionTimerSample;
    }

    public Duration getDuration() {
        return Duration.ofNanos(System.nanoTime() - startTime);
    }

    public void setState(StateTag state) {
        this.state = state;
    }

    @Override
    public StateTag getStateTag() {
        return state;
    }
}
