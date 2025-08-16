package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;

@Configuration
public class CompileAndRunServiceConfig {

    @Bean
    CompileServiceAPIClient compileServiceAPIClient(@Value("${compile-service-hostname}") URI host) {
        return new CompileServiceAPIClient(
                new APIClient(
                        RestClient.create(String.format("http://%s", host))));
    }

    @Bean
    RunnerServiceAPIClient runnerServiceAPIClient(@Value("${runner-service-hostname}") URI host) {
        return new RunnerServiceAPIClient(
                new APIClient(
                        RestClient.create(String.format("http://%s", host))));
    }

    @Bean
    StandardWebSocketClient standardWebSocketClient() {
        return new StandardWebSocketClient();
    }
}
