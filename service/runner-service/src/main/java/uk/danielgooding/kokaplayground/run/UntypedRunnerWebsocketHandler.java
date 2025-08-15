package uk.danielgooding.kokaplayground.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Hashtable;

// TODO: bytes if we're just using json?
@Controller
public class UntypedRunnerWebsocketHandler extends TextWebSocketHandler {
    private final Hashtable<String, RunnerSession> runnerSessions;
    private final ObjectMapper objectMapper;

    private final RunnerWebSocketHandler runnerWebSocketHandler;

    private final int sendTimeLimitMs;
    private final int bufferSizeLimitBytes;

    public UntypedRunnerWebsocketHandler(
            @Autowired Jackson2ObjectMapperBuilder objectMapperBuilder,
            @Autowired RunnerWebSocketHandler runnerWebSocketHandler,
            @Value("${runner.session.send-time-limit-ms:100}") int sendTimeLimitMs,
            @Value("${runner.session.buffer-size-limit-bytes:10000}") int bufferSizeLimitBytes) {
        objectMapper = objectMapperBuilder.build();
        runnerSessions = new Hashtable<>();
        this.runnerWebSocketHandler = runnerWebSocketHandler;
        this.sendTimeLimitMs = sendTimeLimitMs;
        this.bufferSizeLimitBytes = bufferSizeLimitBytes;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        RunnerSession runnerSession = new RunnerSession(
                session, objectMapper, this.sendTimeLimitMs, this.bufferSizeLimitBytes);
        runnerSessions.put(session.getId(), runnerSession);
        runnerWebSocketHandler.handleConnectionEstablished(runnerSession);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage textMessage) throws Exception {
        RunnerSession runnerSession = runnerSessions.get(session.getId());
        RunStreamInbound.Message message =
                objectMapper.readValue(textMessage.asBytes(), RunStreamInbound.Message.class);
        runnerWebSocketHandler.handleMessage(runnerSession, message);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        RunnerSession runnerSession = runnerSessions.remove(session.getId());
        runnerWebSocketHandler.afterConnectionClosed(runnerSession, status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        RunnerSession runnerSession = runnerSessions.get(session.getId());
        runnerWebSocketHandler.handleTransportError(runnerSession, exception);
    }
}
