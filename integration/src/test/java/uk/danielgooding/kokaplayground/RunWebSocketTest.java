package uk.danielgooding.kokaplayground;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.CloseStatus;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.Workdir;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;
import uk.danielgooding.kokaplayground.common.websocket.ITypedWebSocketSession;
import uk.danielgooding.kokaplayground.common.websocket.SessionId;
import uk.danielgooding.kokaplayground.common.websocket.TypedWebSocketSessionAndState;
import uk.danielgooding.kokaplayground.compileandrun.*;
import uk.danielgooding.kokaplayground.protocol.RunStreamInbound;
import uk.danielgooding.kokaplayground.protocol.RunStreamOutbound;
import uk.danielgooding.kokaplayground.run.RunnerService;
import uk.danielgooding.kokaplayground.run.RunnerSessionState;
import uk.danielgooding.kokaplayground.run.RunnerWebSocketHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {TestConfig.class, RunnerService.class, CompileAndRunService.class})
@TestPropertySource(properties = {
        "which-exe-store=local-exe-store",
        // TODO: shouldn't need these
        "local-exe-store.directory=UNUSED",
        "compiler.exe-path=UNUSED",
        "compiler.koka-zero-config-path=UNUSED",
        "runner.bubblewrap-path=UNUSED"})
public class RunWebSocketTest {

    @MockitoBean
    RunnerService runnerServiceMock;

    @MockitoBean
    CompileServiceAPIClient compileServiceAPIClientMock;

    @MockitoBean
    RunnerWebSocketClient runnerWebSocketClientMock;

    @MockitoBean
    ExeStore exeStoreMock;

    @MockitoBean
    @Qualifier("runner-workdir")
    Workdir runnerWorkdirMock;

    // TODO: shouldn't be creating CompileService at all :(
    @MockitoBean
    @Qualifier("compiler-workdir")
    Workdir compilerWorkdirMock;

    @Autowired
    RunnerClientWebSocketHandler runnerClientWebSocketHandler;

    @Autowired
    RunnerWebSocketHandler runnerWebSocketHandler;

    // test subject:
    @Autowired
    CompileAndRunService compileAndRunService;

    @Test
    public void runWebSocketCommunication() throws ExecutionException, InterruptedException, IOException {
        // setup mocks

        TestWebSocketConnection connection =
                new TestWebSocketConnection(
                        runnerWebSocketHandler,
                        runnerClientWebSocketHandler,
                        new SessionId("123"));

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
                            throw new UncheckedIOException(e);
                        }

                        return OrError.ok(null);
                    });
                });

        // act
        OrError<String> runResult = compileAndRunService.compileAndRun(sourceCode).get();

        // assert
        assertThat(runResult).isEqualTo(OrError.ok("hello world :)"));
    }

}
