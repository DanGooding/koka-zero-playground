package uk.danielgooding.kokaplayground.compileandrun;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;

@Configuration
@EnableWebSocket
public class CompileAndRunWebsocketConfig implements WebSocketConfigurer {

    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Autowired
    CompileAndRunWebSocketHandler compileAndRunWebSocketHandler;

    @Autowired
    ConcurrentWebSocketWriteLimits concurrentWebSocketWriteLimits;

    @Value("${compile-and-run-service.allowed-ws-origin}")
    String allowedOrigin;

    @Autowired
    MeterRegistry meterRegistry;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        RealWebSocketHandler realHandler =
                new RealWebSocketHandler(
                        new UntypedWrapperWebSocketHandler<>(
                                compileAndRunWebSocketHandler,
                                CompileAndRunStream.Inbound.Message.class,
                                objectMapperBuilder,
                                meterRegistry),
                        concurrentWebSocketWriteLimits);

        WebSocketHandler handler = new LoggingWebSocketHandlerDecorator(
                new ExceptionWebSocketHandlerDecorator(realHandler));

        registry.addHandler(handler, "/ws/compile-and-run")
                .setAllowedOrigins(allowedOrigin)
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .addInterceptors(new ConcurrencyLimitingInterceptor(meterRegistry));
    }
}
