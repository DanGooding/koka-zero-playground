package uk.danielgooding.kokaplayground;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import uk.danielgooding.kokaplayground.common.Workdir;

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
}
