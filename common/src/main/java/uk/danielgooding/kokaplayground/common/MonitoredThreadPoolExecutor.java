package uk.danielgooding.kokaplayground.common;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.*;

public class MonitoredThreadPoolExecutor {

    public static Executor create(
            MeterRegistry meterRegistry,
            String executorName,
            String threadNamePrefix,
            String propertyPath,
            Environment environment) {

        int queueCapacity =
                environment.getProperty(propertyPath + ".task_queue.capacity", Integer.class, -1);
        BlockingQueue<Runnable> workQueue =
                queueCapacity == -1 ?
                        new LinkedBlockingQueue<>() :
                        new ArrayBlockingQueue<>(queueCapacity);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                environment.getProperty(propertyPath + ".pool.core_size", Integer.class, 1),
                environment.getProperty(propertyPath + ".pool.max_size", Integer.class, Integer.MAX_VALUE),
                environment.getProperty(propertyPath + ".pool.keep_alive_seconds", Integer.class, 60),
                TimeUnit.SECONDS,
                workQueue);

        executor.setThreadFactory(new CustomizableThreadFactory(threadNamePrefix));

        return ExecutorServiceMetrics.monitor(
                meterRegistry, executor, executorName, "custom_executor", Tag.of("name", executorName));
    }

}
