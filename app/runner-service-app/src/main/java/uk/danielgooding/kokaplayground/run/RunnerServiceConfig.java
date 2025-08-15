package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.danielgooding.kokaplayground.common.Workdir;

@Configuration
public class RunnerServiceConfig {
    @Bean
    @Qualifier("runner-workdir")
    public Workdir runnerWorkdir() {
        return new Workdir();
    }
}
