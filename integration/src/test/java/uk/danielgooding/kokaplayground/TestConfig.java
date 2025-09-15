package uk.danielgooding.kokaplayground;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@SpringBootConfiguration
@ComponentScan(basePackages = {
        "uk.danielgooding.kokaplayground"
})
public class TestConfig {

    @Bean
    WebSocketClient nullWebSocketClient() {
        return new WebSocketClient() {
            @Override
            public CompletableFuture<WebSocketSession> execute(@NonNull WebSocketHandler webSocketHandler, @NonNull String uriTemplate, @NonNull Object... uriVariables) {
                throw new UnsupportedOperationException("execute in test bean");
            }

            @Override
            public CompletableFuture<WebSocketSession> execute(@NonNull WebSocketHandler webSocketHandler, @Nullable WebSocketHttpHeaders headers, @NonNull URI uri) {
                throw new UnsupportedOperationException("execute in test bean");
            }
        };
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean(name = "stdin-writer")
    Executor stdinWriterExecutor() {
        return ForkJoinPool.commonPool();
    }

    @Bean(name = "stdout-reader")
    Executor stdoutReaderExecutor() {
        return ForkJoinPool.commonPool();
    }
}
