package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RunnerSessionState {
    private boolean isRunning = false;
    private final BlockingQueue<String> stdinBuffer;

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
        boolean ignoredDropped = stdinBuffer.offer(chunk);
    }

    public BlockingQueue<String> getStdinBuffer() {
        return stdinBuffer;
    }
}
