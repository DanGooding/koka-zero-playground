package uk.danielgooding.kokaplayground.compileandrun;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import uk.danielgooding.kokaplayground.common.MonitoredThreadPoolExecutor;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.*;

@Configuration
public class CompileAndRunServiceConfig {

    @Bean
    CompileServiceAPIClient compileServiceAPIClient(
            @Value("${compile-service-hostname}") URI host,
            MeterRegistry meterRegistry,
            Environment environment) {

        Executor monitoredExecutor = MonitoredThreadPoolExecutor.create(
                meterRegistry,
                "httpClientExecutor",
                "http-client-",
                "executor.http_client",
                environment);

        HttpClient httpClient = HttpClient.newBuilder().executor(monitoredExecutor).build();

        JdkClientHttpRequestFactory clientHttpRequestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        RestClient restClient = RestClient.builder()
                .baseUrl(String.format("http://%s", host))
                .requestFactory(clientHttpRequestFactory)
                .build();
        return new CompileServiceAPIClient(new APIClient(restClient));
    }

    @Bean
    WebSocketClient webSocketClient(MeterRegistry meterRegistry, Environment environment) {

        Executor monitoredExecutor = MonitoredThreadPoolExecutor.create(
                meterRegistry,
                "websocketClientExecutor",
                "websocket-client-",
                "executor.websocket_client",
                environment);

        ConcurrentTaskExecutor asyncExecutor = new ConcurrentTaskExecutor(monitoredExecutor);

        StandardWebSocketClient client = new StandardWebSocketClient();
        client.setTaskExecutor(asyncExecutor);
        return client;
    }
}
