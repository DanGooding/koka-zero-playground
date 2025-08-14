package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Configuration
public class CompileAndRunServiceConfig {

    @Bean
    RestClient compileServiceClient(@Value("${compile-service-hostname}") URI host) {
        return RestClient.create(String.format("http://%s", host));
    }

}
