package uk.danielgooding.kokaplayground;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.websocket.ISessionState;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;

import java.io.IOException;
import java.util.List;


@Service
public class TestCompileAndRunClientWebSocketHandler
        implements TypedWebSocketHandler<
        CompileAndRunStream.Outbound.Message,
        CompileAndRunStream.Inbound.Message,
        TestCompileAndRunClientWebSocketHandler.State,
        Void,
        OrError<String>> {

    public static class State implements ISessionState<Void> {
        private final StringBuilder stdoutBuilder;
        private String maybeError;

        State() {
            stdoutBuilder = new StringBuilder();
        }

        void addStdout(String chunk) {
            stdoutBuilder.append(chunk);
        }

        void setError(String message) {
            maybeError = message;
        }

        OrError<String> getOutcome() {
            if (maybeError == null) {
                return OrError.ok(stdoutBuilder.toString());
            } else {
                return OrError.error(maybeError);
            }
        }

        @Override
        public Void getStateTag() {
            return null;
        }
    }

    @Override
    public State handleConnectionEstablished(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>> session) {
        return new State();
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>> session,
            State state,
            @NonNull CompileAndRunStream.Outbound.Message inbound) {
        if (inbound instanceof CompileAndRunStream.Outbound.Stdout stdout) {
            state.addStdout(stdout.getContent());
        } else if (inbound instanceof CompileAndRunStream.Outbound.Error error) {
            state.setError(error.getMessage());
        }
    }

    @Override
    public OrError<String> afterConnectionClosedOk(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>> session,
            State state) {
        return state.getOutcome();
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>> session,
            State state,
            CloseStatus status) throws IOException {

    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>> session,
            State state,
            Throwable exception) throws IOException {

    }

    @Override
    public boolean isServer() {
        return false;
    }

    @Override
    public Iterable<Void> allSessionStateTags() {
        return List.of();
    }
}
