package uk.danielgooding.kokaplayground.run;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import uk.danielgooding.kokaplayground.common.MonitoredThreadPoolExecutor;
import uk.danielgooding.kokaplayground.common.websocket.WebsocketServerSessionScope;

import java.util.concurrent.*;

@Configuration
public class RunnerServiceConfig {

    @Bean
    public CustomScopeConfigurer customScopeConfigurer(WebsocketServerSessionScope websocketServerSessionScope) {
        CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        configurer.addScope(WebsocketServerSessionScope.REFERENCE, websocketServerSessionScope);

        return configurer;
    }

    @Bean(name = "stdin-writer")
    public Executor stdinWriterExecutor(MeterRegistry meterRegistry, Environment environment) {
        return MonitoredThreadPoolExecutor.create(
                meterRegistry,
                "stdinWriterExecutor",
                "stdin-writer-",
                "runner.stdin-writer-executor",
                environment);
    }

    @Bean(name = "stdout-reader")
    public Executor stdoutReaderExecutor(MeterRegistry meterRegistry, Environment environment) {
        return MonitoredThreadPoolExecutor.create(
                meterRegistry,
                "stdoutReaderExecutor",
                "stdout-reader-",
                "runner.stdout-reader-executor",
                environment);
    }
}
