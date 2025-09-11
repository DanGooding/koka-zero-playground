package uk.danielgooding.kokaplayground.run;

import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import uk.danielgooding.kokaplayground.common.CancellableFuture;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RunnerSessionState {
    private @Nullable CancellableFuture<OrError<Void>> running;
    private final BlockingQueue<String> stdinBuffer;
    private final Timer.Sample sessionSample;

    private static final Logger logger = LoggerFactory.getLogger(RunnerSessionState.class);

    public RunnerSessionState(int maxBufferedItems, Timer.Sample sessionSample) {
        this.stdinBuffer = new ArrayBlockingQueue<>(maxBufferedItems);
        this.sessionSample = sessionSample;
    }

    public boolean isRunning() {
        return running != null && running.isDone();
    }

    public void setRunning(CancellableFuture<OrError<Void>> running) {
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
}
