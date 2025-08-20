package uk.danielgooding.kokaplayground;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;

import java.io.IOException;

@Service
public class TestCompileAndRunClientWebSocketHandler
        implements TypedWebSocketHandler<
        CompileAndRunStream.Outbound.Message,
        CompileAndRunStream.Inbound.Message,
        StringBuilder,
        String> {

    @Override
    public StringBuilder handleConnectionEstablished(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, String> session) {
        return new StringBuilder();
    }

    @Override
    public void handleMessage(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, String> session,
            StringBuilder stdoutBuilder,
            @NonNull CompileAndRunStream.Outbound.Message inbound) {
        if (inbound instanceof CompileAndRunStream.Outbound.Stdout stdout) {
            stdoutBuilder.append(stdout.getContent());
        }
    }

    @Override
    public String afterConnectionClosedOk(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, String> session,
            StringBuilder stdoutBuilder) {
        return stdoutBuilder.toString();
    }

    @Override
    public void afterConnectionClosedErroneously(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, String> session,
            StringBuilder stdoutBuilder,
            CloseStatus status) throws IOException {

    }

    @Override
    public void handleTransportError(
            TypedWebSocketSession<CompileAndRunStream.Inbound.Message, String> session,
            StringBuilder stdoutBuilder,
            Throwable exception) throws IOException {

    }
}
