package uk.danielgooding.kokaplayground.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import uk.danielgooding.kokaplayground.common.CancellableFuture;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RunnerSessionState {
    private @Nullable CancellableFuture<Void> running;
    private final BlockingQueue<String> stdinBuffer;

    private static final Logger logger = LoggerFactory.getLogger(RunnerSessionState.class);

    public RunnerSessionState(int maxBufferedItems) {
        this.stdinBuffer = new ArrayBlockingQueue<>(maxBufferedItems);
    }

    public boolean isRunning() {
        return running != null && running.isDone();
    }

    public void setRunning(CancellableFuture<Void> running) {
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
}
