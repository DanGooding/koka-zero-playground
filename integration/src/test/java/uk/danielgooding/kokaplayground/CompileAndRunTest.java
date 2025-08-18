package uk.danielgooding.kokaplayground;

import org.junit.jupiter.api.AfterEach;
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
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;
import uk.danielgooding.kokaplayground.compile.CompileController;
import uk.danielgooding.kokaplayground.compile.CompileService;
import uk.danielgooding.kokaplayground.compile.CompilerTool;
import uk.danielgooding.kokaplayground.compileandrun.CompileServiceAPIClient;
import uk.danielgooding.kokaplayground.compileandrun.CollectingRunnerWebSocketClient;
import uk.danielgooding.kokaplayground.run.RunnerController;
import uk.danielgooding.kokaplayground.run.RunnerService;
import uk.danielgooding.kokaplayground.run.SandboxedExeRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {TestConfig.class, CompileService.class, RunnerService.class})
@TestPropertySource(properties = {
        "which-exe-store=local-exe-store"})
public class CompileAndRunTest {
    // mocked services:
    @MockitoBean
    CompilerTool compilerToolMock;

    @MockitoBean
    @Qualifier("compiler-workdir")
    Workdir compilerWorkdirMock;

    @MockitoBean
    @Qualifier("runner-workdir")
    Workdir runnerWorkdirMock;

    @MockitoBean
    ExeStore exeStoreMock;

    @MockitoBean
    SandboxedExeRunner exeRunnerMock;

    // unused - mocked to avoid creating a real instance:
    @MockitoBean
    CompileServiceAPIClient compileServiceAPIClientMock;
    @MockitoBean
    CollectingRunnerWebSocketClient runnerWebSocketClientMock;

    // test subjects:
    @Autowired
    CompileController compileController;
    @Autowired
    RunnerController runnerController;

    @Test
    public void typecheckValid() throws ExecutionException, InterruptedException {
        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { println-int(3 + 4); }");

        Mockito.when(compilerToolMock.typecheck(sourceCode)).thenReturn(
                CompletableFuture.completedFuture(OrError.ok(null)));

        OrError<Void> result = compileController.typecheck(sourceCode).get();
        assertThat(result).isInstanceOf(Ok.class);
    }

    @Test
    public void typecheckInvalid() throws ExecutionException, InterruptedException {
        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { println-int(true); }");

        Mockito.when(compilerToolMock.typecheck(sourceCode)).thenReturn(
                CompletableFuture.completedFuture(OrError.error("got bool, expected string")));

        OrError<Void> result = compileController.typecheck(sourceCode).get();
        assertThat(result).isInstanceOf(Failed.class);
    }

    @Test
    public void compileAndRun() throws ExecutionException, InterruptedException, IOException {
        // compile (mock)
        KokaSourceCode sourceCode = new KokaSourceCode("fun main() { println-int(3); }");

        Path preStorePath = Path.of("program.exe");
        Mockito.when(compilerWorkdirMock.freshPath("compile")).thenReturn(preStorePath);

        Mockito.when(compilerToolMock.compile(sourceCode, true)).thenReturn(
                CompletableFuture.completedFuture(OrError.ok(preStorePath)));

        ExeHandle storedHandle = new ExeHandle("stored-program.exe");
        Mockito.when(exeStoreMock.putExe(preStorePath)).thenReturn(storedHandle);

        // compile (act)
        OrError<ExeHandle> compileResult = compileController.compile(sourceCode).get();

        assertThat(compileResult).isEqualTo(OrError.ok(storedHandle));

        // run (mock)
        Path postGetHandle = Path.of("downloaded.exe");
        Mockito.when(exeStoreMock.getExe(storedHandle, runnerWorkdirMock)).thenReturn(OrError.ok(postGetHandle));

        String stdout = "3";
        Mockito.when(
                        exeRunnerMock.runThenGetStdout(
                                ArgumentMatchers.eq(postGetHandle),
                                ArgumentMatchers.eq(List.of()),
                                ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(OrError.ok(
                        stdout)));

        // run (act)
        OrError<String> runResult = runnerController.run(storedHandle).get();

        assertThat(runResult).isEqualTo(OrError.ok(stdout));
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(
                compilerToolMock,
                exeStoreMock,
                exeRunnerMock,
                compilerWorkdirMock,
                runnerWorkdirMock);
    }
}
