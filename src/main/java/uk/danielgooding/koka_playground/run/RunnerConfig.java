package uk.danielgooding.koka_playground.run;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.danielgooding.koka_playground.common.Workdir;

@Configuration
public class RunnerConfig {
    @Bean
    @Qualifier("runner-workdir")
    public Workdir runnerWorkdir() {
        return new Workdir();
    }

    @Bean
    public ExeRunner exeRunner() {
        return new SandboxedExeRunner();
    }
}
