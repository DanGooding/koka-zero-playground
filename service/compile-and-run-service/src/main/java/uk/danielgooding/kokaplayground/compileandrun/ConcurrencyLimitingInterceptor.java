package uk.danielgooding.kokaplayground.compileandrun;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limit.VegasLimit;
import com.netflix.concurrency.limits.limiter.AbstractLimiter;
import com.netflix.concurrency.limits.limiter.SimpleLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

public class ConcurrencyLimitingInterceptor implements HandshakeInterceptor {
    public static final String listenerAttributeName = "ConcurrencyLimitingInterceptor:listener";
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyLimitingInterceptor.class);

    private final Limiter<Void> limiter;

    public ConcurrencyLimitingInterceptor(MeterRegistry meterRegistry) {
        limiter = SimpleLimiter.newBuilder()
                .limit(VegasLimit.newBuilder()
                        .initialLimit(10)
                        .maxConcurrency(40)
                        .build())
                .metricRegistry(
                        new MicrometerMetricRegistry(meterRegistry, "concurrency_limiting_interceptor_"))
                .build();
    }

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        Optional<Limiter.Listener> maybeListener = limiter.acquire(null);
        if (maybeListener.isEmpty()) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        } else {
            Limiter.Listener listener = maybeListener.get();
            attributes.put(listenerAttributeName, listener);
            return true;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception) {
    }
}
