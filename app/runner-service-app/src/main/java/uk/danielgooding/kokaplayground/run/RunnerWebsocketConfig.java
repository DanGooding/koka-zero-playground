package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import uk.danielgooding.kokaplayground.common.websocket.ConcurrentWebSocketWriteLimits;
import uk.danielgooding.kokaplayground.common.websocket.UntypedWrapperWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.RunStreamInbound;
import uk.danielgooding.kokaplayground.protocol.RunStreamOutbound;

@Configuration
@EnableWebSocket
public class RunnerWebsocketConfig implements WebSocketConfigurer {

    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Autowired
    RunnerWebSocketHandler runnerWebSocketHandler;

    @Autowired
    ConcurrentWebSocketWriteLimits concurrentWebSocketWriteLimits;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        UntypedWrapperWebSocketHandler<RunStreamInbound.Message, RunStreamOutbound.Message, RunnerSessionState, Void> untypedRunnerWebsocketHandler =
                new UntypedWrapperWebSocketHandler<>(
                        runnerWebSocketHandler,
                        RunStreamInbound.Message.class,
                        objectMapperBuilder,
                        concurrentWebSocketWriteLimits);

        WebSocketHandler handler = new LoggingWebSocketHandlerDecorator(
                new ExceptionWebSocketHandlerDecorator(untypedRunnerWebsocketHandler));

        registry.addHandler(handler, "/ws/run")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
