package uk.danielgooding.kokaplayground.run;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public Executor executor(MeterRegistry meterRegistry) {
        // TODO: configure in environment (core, max, keepalive, max-capacity)
        // TODO: shared configuration for all services

        BlockingQueue<Runnable> workQueue =
                new LinkedBlockingQueue<>();
        Executor executor =
                new ThreadPoolExecutor(8, 32, 60, TimeUnit.SECONDS, workQueue);

        return ExecutorServiceMetrics.monitor(meterRegistry, executor, "completableFutureExecutor", "futures");
    }
}
