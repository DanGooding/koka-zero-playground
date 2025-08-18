package uk.danielgooding.kokaplayground.compileandrun;

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
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;

@Configuration
@EnableWebSocket
public class CompileAndRunWebsocketConfig implements WebSocketConfigurer {

    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Autowired
    CompileAndRunWebSocketHandler compileAndRunWebSocketHandler;

    @Autowired
    ConcurrentWebSocketWriteLimits concurrentWebSocketWriteLimits;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        UntypedWrapperWebSocketHandler<CompileAndRunStream.Inbound.Message, CompileAndRunStream.Outbound.Message, CompileAndRunSessionState, Void> untypedRunnerWebsocketHandler =
                new UntypedWrapperWebSocketHandler<>(
                        compileAndRunWebSocketHandler,
                        CompileAndRunStream.Inbound.Message.class,
                        objectMapperBuilder,
                        concurrentWebSocketWriteLimits);

        WebSocketHandler handler = new LoggingWebSocketHandlerDecorator(
                new ExceptionWebSocketHandlerDecorator(untypedRunnerWebsocketHandler));

        registry.addHandler(handler, "/ws/compile-and-run")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
