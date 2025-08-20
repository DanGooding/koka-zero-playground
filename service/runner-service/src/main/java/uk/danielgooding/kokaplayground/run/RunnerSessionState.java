package uk.danielgooding.kokaplayground.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RunnerSessionState {
    private boolean isRunning = false;
    private final BlockingQueue<String> stdinBuffer;

    private static final Logger logger = LoggerFactory.getLogger(RunnerSessionState.class);

    public RunnerSessionState(int maxBufferedItems) {
        this.stdinBuffer = new ArrayBlockingQueue<>(maxBufferedItems);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public void bufferOrDropStdin(String chunk) {
        boolean ignored = stdinBuffer.offer(chunk);
    }

    public BlockingQueue<String> getStdinBuffer() {
        return stdinBuffer;
    }
}
