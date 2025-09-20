package uk.danielgooding.kokaplayground.run;

import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import uk.danielgooding.kokaplayground.common.CancellableFuture;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ISessionState;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RunnerSessionState implements ISessionState<RunnerSessionState.StateTag> {
    private @Nullable CancellableFuture<OrError<RunStats>> running;
    private final BlockingQueue<String> stdinBuffer;
    private final Timer.Sample sessionSample;
    private StateTag state = StateTag.AWAITING_RUN;

    private static final Logger logger = LoggerFactory.getLogger(RunnerSessionState.class);

    public enum StateTag {
        AWAITING_REQUEST,
        AWAITING_RUN,
        RUNNING,
        COMPLETE
    }

    public RunnerSessionState(int maxBufferedItems, Timer.Sample sessionSample) {
        this.stdinBuffer = new ArrayBlockingQueue<>(maxBufferedItems);
        this.sessionSample = sessionSample;
    }

    public boolean isRunning() {
        return running != null && running.isDone();
    }

    public void setRunning(CancellableFuture<OrError<RunStats>> running) {
        this.running = running;
    }

    public void cancelCurrentRun() {
        if (running != null) running.cancel();
    }

    public void bufferOrDropStdin(String chunk) {
        boolean ignored = stdinBuffer.offer(chunk);
    }

    public BlockingQueue<String> getStdinBuffer() {
        return stdinBuffer;
    }

    public Timer.Sample getSessionSample() {
        return sessionSample;
    }

    public void setState(StateTag state) {
        this.state = state;
    }

    @Override
    public StateTag getStateTag() {
        return state;
    }
}
