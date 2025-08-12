package uk.danielgooding.koka_playground.compile;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.danielgooding.koka_playground.common.Workdir;

@Configuration
public class CompileServiceConfig {
    @Bean
    @Qualifier("compiler-workdir")
    public Workdir compilerWorkdir() {
        return new Workdir();
    }
}
