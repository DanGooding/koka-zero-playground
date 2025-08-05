package uk.danielgooding.koka_playground;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    @Qualifier("compiler-workdir")
    public Workdir compilerWorkdir() {
        return new Workdir();
    }

    @Bean
    @Qualifier("runner-workdir")
    public Workdir runnerWorkdir() {
        return new Workdir();
    }
}
