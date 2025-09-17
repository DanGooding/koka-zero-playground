package uk.danielgooding.kokaplayground;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.danielgooding.kokaplayground.common.*;
import uk.danielgooding.kokaplayground.common.exe.ExeStore;
import uk.danielgooding.kokaplayground.compile.CompileController;
import uk.danielgooding.kokaplayground.compile.CompileService;
import uk.danielgooding.kokaplayground.compile.CompilerTool;
import uk.danielgooding.kokaplayground.compile.ExeCacheKey;
import uk.danielgooding.kokaplayground.compileandrun.CompileServiceAPIClient;
import uk.danielgooding.kokaplayground.run.RunnerService;
import uk.danielgooding.kokaplayground.run.SandboxedExeRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {TestConfig.class, CompileService.class, RunnerService.class})
@TestPropertySource(properties = {
        "runner-service-hostname=UNUSED",
        "runner.max-buffered-stdin-items=10",
        "runner.max-stderr-bytes=100",
        "compiler.version-hash=ABCD"})
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

    @MockitoBean
    MeterRegistry meterRegistry;

    // unused - mocked to avoid creating a real instance:
    @MockitoBean
    CompileServiceAPIClient compileServiceAPIClientMock;

    // unused - mocked to avoid creating a real instance:
    @MockitoBean("exeCacheRedisTemplate")
    RedisTemplate<ExeCacheKey, byte[]> exeCacheKeyRedisTemplate;

    // test subjects:
    @Autowired
    CompileController compileController;

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
