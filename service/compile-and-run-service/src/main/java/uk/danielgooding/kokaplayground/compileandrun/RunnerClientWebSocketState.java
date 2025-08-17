package uk.danielgooding.kokaplayground.compileandrun;

import uk.danielgooding.kokaplayground.common.Failed;
import uk.danielgooding.kokaplayground.common.Ok;
import uk.danielgooding.kokaplayground.common.OrError;

public class RunnerClientWebSocketState {
    private final StringBuilder stdoutBuilder;
    private OrError<Void> result;

    public RunnerClientWebSocketState() {
        this.stdoutBuilder = new StringBuilder();
        this.result = null;
    }

    public void appendStdout(String chunk) {
        stdoutBuilder.append(chunk);
    }

    public void setResult(OrError<Void> result) {
        this.result = result;
    }

    public OrError<String> getOutcome() {
        return switch (result) {
            case Ok<Void> ok -> OrError.ok(stdoutBuilder.toString());
            case Failed<?> failed -> failed.castValue();
        };
    }
}
