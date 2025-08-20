package uk.danielgooding.kokaplayground;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;
import uk.danielgooding.kokaplayground.common.websocket.SessionId;
import uk.danielgooding.kokaplayground.compileandrun.*;
import uk.danielgooding.kokaplayground.protocol.RunStream;
import uk.danielgooding.kokaplayground.run.RunnerService;
import uk.danielgooding.kokaplayground.run.RunnerSessionState;
import uk.danielgooding.kokaplayground.run.RunnerWebSocketHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {TestConfig.class, RunnerService.class, CompileAndRunService.class})
@TestPropertySource(properties = {
        "local-exe-store.directory=UNUSED",
        "compiler.exe-path=UNUSED",
        "compiler.koka-zero-config-path=UNUSED",
        "runner.bubblewrap-path=UNUSED",
        "runner-service-hostname=UNUSED"})
public class RunWebSocketTest {

    // mocked services
    @MockitoBean
    RunnerService runnerServiceMock;

    @MockitoBean
    CompileServiceAPIClient compileServiceAPIClientMock;

    // mocked to attach the test websocket
    @MockitoBean
    CollectingRunnerWebSocketClient runnerWebSocketClientMock;

    @MockitoBean
    ExeStore exeStoreMock;

    // test subjects:
    @Autowired
    CollectingRunnerClientWebSocketHandler runnerClientWebSocketHandler;

    @Autowired
    RunnerWebSocketHandler runnerWebSocketHandler;

    @Autowired
    CompileAndRunService compileAndRunService;

    TestWebSocketConnection<
            RunStream.Inbound.Message,
            RunStream.Outbound.Message,
            CollectingRunnerClientWebSocketState,
            RunnerSessionState,
            OrError<String>>
    createRunnerConnection() {
        return new TestWebSocketConnection<>(
                runnerWebSocketHandler,
                runnerClientWebSocketHandler,
                RunStream.Inbound.Message.class,
                RunStream.Outbound.Message.class,
                new SessionId("123"));
    }

    @Test
    public void runWebSocketCommunication() throws ExecutionException, InterruptedException, IOException {
        // setup mocks

        var connection = createRunnerConnection();

        Mockito.when(runnerWebSocketClientMock.execute()).thenAnswer(invocation -> {
            connection.establishConnection();
            return CompletableFuture.completedFuture(connection.getClientSessionAndState());
        });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runWithoutStdinStreamingStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Callback<Void> onStart = invocation.getArgument(1);
                    Callback<String> onStdout = invocation.getArgument(2);

                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            onStart.call(null);

                            onStdout.call("hello ");
                            onStdout.call("world ");
                            onStdout.call(":)");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        return OrError.ok(null);
                    });
                });

        // act
        OrError<String> runResult = compileAndRunService.compileAndRun(sourceCode).get();

        // assert
        assertThat(runResult).isEqualTo(OrError.ok("hello world :)"));
    }

    @Test
    public void breakConnectionWhileRunning() throws IOException {
        // setup mocks

        var connection = createRunnerConnection();

        Mockito.when(runnerWebSocketClientMock.execute()).thenAnswer(invocation -> {
            connection.establishConnection();
            return CompletableFuture.completedFuture(connection.getClientSessionAndState());
        });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runWithoutStdinStreamingStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Callback<Void> onStart = invocation.getArgument(1);
                    Callback<String> onStdout = invocation.getArgument(2);

                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            onStart.call(null);

                            onStdout.call("hello...");
                            // break the connection before we complete this stage
                            connection.breakConnection();
                            onStdout.call("...world");

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        return OrError.ok(null);
                    });
                });

        // act
        CompletableFuture<OrError<String>> runResult = compileAndRunService.compileAndRun(sourceCode);

        // assert future is failed
        assertThrows(ExecutionException.class, runResult::get);
    }

    @Test
    public void raiseDuringRunnerService() throws IOException {
        // setup mocks

        var connection = createRunnerConnection();

        Mockito.when(runnerWebSocketClientMock.execute()).thenAnswer(invocation -> {
            connection.establishConnection();
            return CompletableFuture.completedFuture(connection.getClientSessionAndState());
        });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runWithoutStdinStreamingStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenAnswer(invocation -> CompletableFuture.supplyAsync(() -> {
                    throw new RuntimeException("failure in runner service");
                }));

        // act
        CompletableFuture<OrError<String>> runResult = compileAndRunService.compileAndRun(sourceCode);

        // assert
        assertThrows(ExecutionException.class, runResult::get);
    }


    @Test
    public void clientErrorInRunnerService() throws IOException, ExecutionException, InterruptedException {
        // setup mocks

        var connection = createRunnerConnection();

        Mockito.when(runnerWebSocketClientMock.execute()).thenAnswer(invocation -> {
            connection.establishConnection();
            return CompletableFuture.completedFuture(connection.getClientSessionAndState());
        });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runWithoutStdinStreamingStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Callback<Void> onStart = invocation.getArgument(1);
                    Callback<String> onStdout = invocation.getArgument(2);

                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            onStart.call(null);

                            onStdout.call("hello...");

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        return OrError.error("your code was bad");
                    });
                });

        // act
        OrError<String> runResult = compileAndRunService.compileAndRun(sourceCode).get();

        // assert
        assertThat(runResult).isInstanceOf(Failed.class);
    }

    @Test
    public void runnerStateRaises() throws IOException {
        // setup mocks

        var connection = createRunnerConnection();

        Mockito.when(runnerWebSocketClientMock.execute()).thenAnswer(invocation -> {
            connection.establishConnection();
            return CompletableFuture.completedFuture(connection.getClientSessionAndState());
        });

        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { ... }");
        ExeHandle exeHandle = new ExeHandle("the exe");
        Mockito.when(compileServiceAPIClientMock.compile(sourceCode))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(exeHandle)));

        Mockito.when(exeStoreMock.getExe(ArgumentMatchers.eq(exeHandle), ArgumentMatchers.any()))
                .thenReturn(OrError.ok(Path.of("/path/to/exe")));

        Mockito.when(runnerServiceMock.runWithoutStdinStreamingStdout(
                        ArgumentMatchers.eq(exeHandle),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("error in runner service"));

        // act
        CompletableFuture<OrError<String>> runResult = compileAndRunService.compileAndRun(sourceCode);

        // assert
        assertThat(runResult).isCompletedExceptionally();
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
