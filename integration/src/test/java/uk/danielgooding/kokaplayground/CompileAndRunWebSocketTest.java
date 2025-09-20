package uk.danielgooding.kokaplayground;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;
import uk.danielgooding.kokaplayground.common.websocket.SessionId;
import uk.danielgooding.kokaplayground.common.websocket.StatelessTypedWebSocketHandler;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.compile.ExeCacheKey;
import uk.danielgooding.kokaplayground.compileandrun.*;
import uk.danielgooding.kokaplayground.protocol.CompileAndRunStream;
import uk.danielgooding.kokaplayground.protocol.RunStream;
import uk.danielgooding.kokaplayground.run.RunnerService;
import uk.danielgooding.kokaplayground.run.RunnerSessionState;
import uk.danielgooding.kokaplayground.run.RunnerWebSocketHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {TestConfig.class, RunnerService.class})
@TestPropertySource(properties = {
        "local-exe-store.directory=UNUSED",
        "compiler.exe-path=UNUSED",
        "compiler.koka-zero-config-path=UNUSED",
        "compiler.version-hash=UNUSED",
        "runner.bubblewrap-path=UNUSED",
        "runner-service-hostname=UNUSED",
        "runner.max-buffered-stdin-items=10",
        "runner.max-stderr-bytes=100"})
public class CompileAndRunWebSocketTest {
    // mocked services
    @MockitoBean
    RunnerService runnerServiceMock;

    @MockitoBean
    CompileServiceAPIClient compileServiceAPIClientMock;

    // mocked to attach the test websocket
    @MockitoBean
    ProxyingRunnerWebSocketClient runnerWebSocketClientMock;

    @MockitoBean
    ExeStore exeStoreMock;

    // unused - mocked to avoid creating a real instance:
    @MockitoBean("exeCacheRedisTemplate")
    RedisTemplate<ExeCacheKey, byte[]> exeCacheKeyRedisTemplate;

    // test subjects:
    // session -> TestCompileAndRunClientWebSocketHandler
    // -> CompileAndRunWebSocketHandler -> ProxyingRunnerClientWebSocketClient -> ProxyingRunnerClientWebSocketHandler
    // RunnerWebSocketHandler -> RunnerService
    @Autowired
    RunnerWebSocketHandler runnerWebSocketHandler;

    @Autowired
    CompileAndRunWebSocketHandler compileAndRunWebSocketHandler;

    @Autowired
    TestCompileAndRunClientWebSocketHandler compileAndRunClientWebSocketHandler;

    TestWebSocketConnection<
            RunStream.Inbound.Message,
            RunStream.Outbound.Message,
            StatelessTypedWebSocketHandler.EmptyState,
            Void,
            RunnerSessionState,
            RunnerSessionState.StateTag,
            Void>
    createRunnerConnection(ProxyingRunnerClientState proxyingRunnerClientState) {
        var runnerClientWebSocketHandler = new ProxyingRunnerClientWebSocketHandler(proxyingRunnerClientState);

        return new TestWebSocketConnection<>(
                runnerWebSocketHandler,
                new StatelessTypedWebSocketHandler<>(runnerClientWebSocketHandler),
                RunStream.Inbound.Message.class,
                RunStream.Outbound.Message.class,
                new SessionId("compile-and-run->run"));
    }

    TestWebSocketConnection<
            CompileAndRunStream.Inbound.Message,
            CompileAndRunStream.Outbound.Message,
            TestCompileAndRunClientWebSocketHandler.State,
            Void,
            CompileAndRunSessionState,
            CompileAndRunSessionState.StateTag,
            OrError<String>>
    createCompileAndRunConnection() {
        return new TestWebSocketConnection<>(
                compileAndRunWebSocketHandler,
                compileAndRunClientWebSocketHandler,
                CompileAndRunStream.Inbound.Message.class,
                CompileAndRunStream.Outbound.Message.class,
                new SessionId("client->compile-and-run"));
    }

    @Test
    public void runWebSocketCommunication() throws ExecutionException, InterruptedException, IOException {
        // setup mocks

        var compileAndRunConnection = createCompileAndRunConnection();

        Mockito.when(runnerWebSocketClientMock.execute(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    ProxyingRunnerClientState proxyingRunnerClientState = invocation.getArgument(0);

                    var runnerConnection = createRunnerConnection(proxyingRunnerClientState);
                    runnerConnection.establishConnection();

                    return CompletableFuture.completedFuture(runnerConnection.getClientSessionAndState().getSession());
                });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runStreamingStdinAndStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    BlockingQueue<String> stdinBuffer = invocation.getArgument(1);
                    Callback<Void> onStart = invocation.getArgument(2);
                    Callback<String> onStdout = invocation.getArgument(3);

                    return CancellableFuture.supplyAsync((canceler) -> {
                        try {
                            onStart.call(null);

                            String name = stdinBuffer.take();
                            onStdout.call(String.format("hello, %s!", name));
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        return OrCancelled.ok(OrError.ok(null));
                    }, ForkJoinPool.commonPool());
                });

        // act

        // pretend to be a client of CompileAndRunService
        compileAndRunConnection.establishConnection();
        TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>>
                compileAndRunClientSession = compileAndRunConnection.getClientSessionAndState().getSession();

        compileAndRunClientSession.sendMessage(new CompileAndRunStream.Inbound.CompileAndRun(sourceCode));

        // send some stdin
        compileAndRunClientSession.sendMessage(new CompileAndRunStream.Inbound.Stdin("koka"));

        CompletableFuture<OrError<String>> outcomeStdout = compileAndRunClientSession.getOutcomeFuture();

        // assert
        assertThat(outcomeStdout.get()).isEqualTo(OrError.ok("hello, koka!"));
    }

    @Test
    public void breakConnectionToRunner() throws ExecutionException, InterruptedException, IOException {
        // setup mocks

        var compileAndRunConnection = createCompileAndRunConnection();
        AtomicReference<Runnable> breakRunnerConnection = new AtomicReference<>();

        Mockito.when(runnerWebSocketClientMock.execute(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    ProxyingRunnerClientState proxyingRunnerClientState = invocation.getArgument(0);

                    var runnerConnection = createRunnerConnection(proxyingRunnerClientState);
                    runnerConnection.establishConnection();
                    breakRunnerConnection.set(() ->
                    {
                        try {
                            runnerConnection.breakConnection();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    return CompletableFuture.completedFuture(runnerConnection.getClientSessionAndState().getSession());
                });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runStreamingStdinAndStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    BlockingQueue<String> stdinBuffer = invocation.getArgument(1);
                    Callback<Void> onStart = invocation.getArgument(2);
                    Callback<String> onStdout = invocation.getArgument(3);

                    return CancellableFuture.supplyAsync((canceler) -> {
                        try {
                            onStart.call(null);

                            String name = stdinBuffer.take();
                            breakRunnerConnection.get().run();

                            onStdout.call(String.format("hello, %s!", name));
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        return OrCancelled.ok(OrError.ok(null));
                    }, ForkJoinPool.commonPool());
                });

        // act

        // pretend to be a client of CompileAndRunService
        compileAndRunConnection.establishConnection();
        TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>>
                compileAndRunClientSession = compileAndRunConnection.getClientSessionAndState().getSession();

        compileAndRunClientSession.sendMessage(new CompileAndRunStream.Inbound.CompileAndRun(sourceCode));

        // send some stdin
        compileAndRunClientSession.sendMessage(new CompileAndRunStream.Inbound.Stdin("koka"));

        CompletableFuture<OrError<String>> outcomeStdout = compileAndRunClientSession.getOutcomeFuture();

        // assert
        assertThrows(ExecutionException.class, outcomeStdout::get);
    }

    @Test
    public void raiseInRunnerService() throws ExecutionException, InterruptedException, IOException {
        // setup mocks

        var compileAndRunConnection = createCompileAndRunConnection();

        Mockito.when(runnerWebSocketClientMock.execute(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    ProxyingRunnerClientState proxyingRunnerClientState = invocation.getArgument(0);

                    var runnerConnection = createRunnerConnection(proxyingRunnerClientState);
                    runnerConnection.establishConnection();

                    return CompletableFuture.completedFuture(runnerConnection.getClientSessionAndState().getSession());
                });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runStreamingStdinAndStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    BlockingQueue<String> stdinBuffer = invocation.getArgument(1);
                    Callback<Void> onStart = invocation.getArgument(2);
                    Callback<String> onStdout = invocation.getArgument(3);

                    return CancellableFuture.supplyAsync((canceler) -> {
                        try {
                            onStart.call(null);

                            throw new RuntimeException("simulate failure in runner");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, ForkJoinPool.commonPool());
                });

        // act

        // pretend to be a client of CompileAndRunService
        compileAndRunConnection.establishConnection();
        TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>>
                compileAndRunClientSession = compileAndRunConnection.getClientSessionAndState().getSession();

        compileAndRunClientSession.sendMessage(new CompileAndRunStream.Inbound.CompileAndRun(sourceCode));

        CompletableFuture<OrError<String>> outcomeStdout = compileAndRunClientSession.getOutcomeFuture();

        // assert
        assertThrows(ExecutionException.class, outcomeStdout::get);
    }

    @Test
    public void clientErrorInRunnerService() throws ExecutionException, InterruptedException, IOException {
        // setup mocks

        var compileAndRunConnection = createCompileAndRunConnection();

        Mockito.when(runnerWebSocketClientMock.execute(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    ProxyingRunnerClientState proxyingRunnerClientState = invocation.getArgument(0);

                    var runnerConnection = createRunnerConnection(proxyingRunnerClientState);
                    runnerConnection.establishConnection();

                    return CompletableFuture.completedFuture(runnerConnection.getClientSessionAndState().getSession());
                });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runStreamingStdinAndStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    BlockingQueue<String> stdinBuffer = invocation.getArgument(1);
                    Callback<Void> onStart = invocation.getArgument(2);
                    Callback<String> onStdout = invocation.getArgument(3);

                    return CancellableFuture.supplyAsync((canceler) -> {
                        try {
                            onStart.call(null);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        return OrCancelled.ok(OrError.error("your code was bad"));
                    }, ForkJoinPool.commonPool());
                });

        // act

        // pretend to be a client of CompileAndRunService
        compileAndRunConnection.establishConnection();
        TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>>
                compileAndRunClientSession = compileAndRunConnection.getClientSessionAndState().getSession();

        compileAndRunClientSession.sendMessage(new CompileAndRunStream.Inbound.CompileAndRun(sourceCode));

        CompletableFuture<OrError<String>> outcomeStdout = compileAndRunClientSession.getOutcomeFuture();

        // assert
        assertThat(outcomeStdout.get()).isInstanceOf(Failed.class);
    }

    @Test
    public void runnerStateRaises() throws ExecutionException, InterruptedException, IOException {
        // setup mocks

        var compileAndRunConnection = createCompileAndRunConnection();

        Mockito.when(runnerWebSocketClientMock.execute(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    ProxyingRunnerClientState proxyingRunnerClientState = invocation.getArgument(0);

                    var runnerConnection = createRunnerConnection(proxyingRunnerClientState);
                    runnerConnection.establishConnection();

                    return CompletableFuture.completedFuture(runnerConnection.getClientSessionAndState().getSession());
                });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runStreamingStdinAndStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("simulate error in runner service"));

        // act

        // pretend to be a client of CompileAndRunService
        compileAndRunConnection.establishConnection();
        TypedWebSocketSession<CompileAndRunStream.Inbound.Message, OrError<String>>
                compileAndRunClientSession = compileAndRunConnection.getClientSessionAndState().getSession();

        compileAndRunClientSession.sendMessage(new CompileAndRunStream.Inbound.CompileAndRun(sourceCode));

        CompletableFuture<OrError<String>> outcomeStdout = compileAndRunClientSession.getOutcomeFuture();

        // assert
        assertThat(outcomeStdout).isCompletedExceptionally();
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(
                runnerServiceMock,
                runnerWebSocketClientMock,
                compileServiceAPIClientMock,
                exeStoreMock);
    }

}
