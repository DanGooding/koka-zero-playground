package uk.danielgooding.kokaplayground.compileandrun;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import uk.danielgooding.kokaplayground.common.websocket.*;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


@Service
public class ProxyingRunnerWebSocketClient {
    private final String uri;
    private final TypedWebSocketClient<
            RunStream.Outbound.Message,
            RunStream.Inbound.Message,
            StatelessTypedWebSocketHandler.EmptyState,
            Void,
            ProxyingRunnerClientState,
            Void> client;

    private static final Logger logger = LoggerFactory.getLogger(ProxyingRunnerWebSocketClient.class);

    public ProxyingRunnerWebSocketClient(
            @Autowired WebSocketClient webSocketClient,
            @Value("${runner-service-hostname}") URI host,
            @Autowired ProxyingRunnerClientWebSocketHandlerFactory handlerFactory,
            @Autowired Jackson2ObjectMapperBuilder objectMapperBuilder,
            @Autowired ConcurrentWebSocketWriteLimits writeLimits,
            @Autowired MeterRegistry meterRegistry) {

        Function<
                ProxyingRunnerClientState,
                TypedWebSocketHandler<
                        RunStream.Outbound.Message,
                        RunStream.Inbound.Message,
                        StatelessTypedWebSocketHandler.EmptyState,
                        Void,
                        Void>
                > handlerBuilder =
                (ProxyingRunnerClientState downstreamSessionAndState) -> {
                    handlerFactory.setDownstreamSessionAndState(downstreamSessionAndState);
                    return new StatelessTypedWebSocketHandler<>(handlerFactory.getObject());
                };

        this.client = new TypedWebSocketClient<>(
                webSocketClient,
                handlerBuilder,
                RunStream.Outbound.Message.class,
                objectMapperBuilder,
                writeLimits,
                meterRegistry);
        this.uri = String.format("ws://%s/ws/run", host);
    }

    public CompletableFuture<TypedWebSocketSession<RunStream.Inbound.Message, Void>>
    execute(ProxyingRunnerClientState downstreamSessionAndState) {
        logger.info("connecting to Runner service {} from downstream {}", uri, downstreamSessionAndState.getSession());
        return client.execute(uri, downstreamSessionAndState);
    }
}
