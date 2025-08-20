package uk.danielgooding.kokaplayground;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import uk.danielgooding.kokaplayground.common.Workdir;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@SpringBootConfiguration
@ComponentScan(basePackages = {
        "uk.danielgooding.kokaplayground"
})
public class TestConfig {

    @Bean
    @Qualifier("runner-workdir")
    Workdir runnerWorkdir() {
        return new Workdir();
    }

    @Bean
    @Qualifier("compiler-workdir")
    Workdir compilerWorkdir() {
        return new Workdir();
    }

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
}
