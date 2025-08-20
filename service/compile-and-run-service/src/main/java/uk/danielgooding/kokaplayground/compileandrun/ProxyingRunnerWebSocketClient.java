package uk.danielgooding.kokaplayground.compileandrun;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import uk.danielgooding.kokaplayground.common.websocket.*;
import uk.danielgooding.kokaplayground.protocol.RunStream;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


@Service
@Scope("prototype")
public class ProxyingRunnerWebSocketClient {
    private static final Log log = LogFactory.getLog(ProxyingRunnerWebSocketClient.class);
    private final String uri;
    private final TypedWebSocketClient<
            RunStream.Outbound.Message,
            RunStream.Inbound.Message,
            Void,
            ProxyingRunnerClientState,
            Void> client;

    public ProxyingRunnerWebSocketClient(
            @Autowired WebSocketClient webSocketClient,
            @Value("${runner-service-hostname}") URI host,
            @Autowired ProxyingRunnerClientWebSocketHandlerFactory handlerFactory,
            @Autowired Jackson2ObjectMapperBuilder objectMapperBuilder,
            @Autowired ConcurrentWebSocketWriteLimits writeLimits) {

        Function<
                ProxyingRunnerClientState,
                TypedWebSocketHandler<
                        RunStream.Outbound.Message,
                        RunStream.Inbound.Message,
                        Void,
                        Void>
                > handlerBuilder =
                (ProxyingRunnerClientState downstreamSessionAndState) -> {
                    handlerFactory.setDownstreamSessionAndState(downstreamSessionAndState);
                    return handlerFactory.getObject();
                };

        this.client = new TypedWebSocketClient<>(
                webSocketClient, handlerBuilder, RunStream.Outbound.Message.class, objectMapperBuilder, writeLimits);
        this.uri = String.format("ws://%s/ws/run", host);
    }

    public CompletableFuture<TypedWebSocketSession<RunStream.Inbound.Message, Void>>
    execute(ProxyingRunnerClientState downstreamSessionAndState) {
        log.info(String.format("connecting to Runner service %s from downstream %s", uri, downstreamSessionAndState.getSession()));
        return client.execute(uri, downstreamSessionAndState);
    }
}
