package uk.danielgooding.kokaplayground.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record ConcurrentWebSocketWriteLimits(@Value("${websocket.session.send-time-limit-ms:100}") int sendTimeLimitMs,
                                             @Value("${websocket.session.buffer-size-limit-bytes:10000}") int bufferSizeLimitBytes) {
}
