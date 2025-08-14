package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.danielgooding.kokaplayground.common.ExeHandle;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.CompletableFuture;

@RestController
public class CompileAndRunController {

    @Autowired
    CompileAndRunService compileAndRunService;

    @PostMapping("/typecheck")
    public CompletableFuture<OrError<Void>> typecheck(@RequestBody KokaSourceCode sourceCode) {
        throw new RuntimeException("/typecheck: TODO: call CompileService");
    }

    @PostMapping("/compile-and-run")
    public CompletableFuture<OrError<ExeHandle>> compileAndRun(@RequestBody KokaSourceCode sourceCode) {
        return compileAndRunService.compileAndRun(sourceCode);
    }
}
