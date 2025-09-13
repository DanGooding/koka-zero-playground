package uk.danielgooding.kokaplayground.run;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
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
import uk.danielgooding.kokaplayground.common.websocket.RealWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.UntypedWrapperWebSocketHandler;
import uk.danielgooding.kokaplayground.protocol.RunStream;

@Configuration
@EnableWebSocket
public class RunnerWebsocketConfig implements WebSocketConfigurer {

    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Autowired
    RunnerWebSocketHandler runnerWebSocketHandler;

    @Autowired
    ConcurrentWebSocketWriteLimits concurrentWebSocketWriteLimits;

    @Autowired
    MeterRegistry meterRegistry;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        RealWebSocketHandler realHandler =
                new RealWebSocketHandler(
                        new UntypedWrapperWebSocketHandler<>(
                                runnerWebSocketHandler,
                                RunStream.Inbound.Message.class,
                                objectMapperBuilder,
                                meterRegistry),
                        concurrentWebSocketWriteLimits);

        WebSocketHandler handler = new LoggingWebSocketHandlerDecorator(
                new ExceptionWebSocketHandlerDecorator(realHandler));

        registry.addHandler(handler, "/ws/run")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
