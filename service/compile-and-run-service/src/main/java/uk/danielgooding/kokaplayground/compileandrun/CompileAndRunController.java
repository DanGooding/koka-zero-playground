package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.CompletableFuture;

@RestController
public class CompileAndRunController {

    @Autowired
    CompileAndRunService compileAndRunService;

    @Autowired
    CompileServiceAPIClient compileServiceAPIClient;

    @PostMapping("/typecheck")
    public CompletableFuture<OrError<Void>> typecheck(@RequestBody KokaSourceCode sourceCode) {
        return compileServiceAPIClient.typecheck(sourceCode);
    }

    @PostMapping("/compile-and-run")
    public CompletableFuture<OrError<String>> compileAndRun(@RequestBody KokaSourceCode sourceCode) {
        return compileAndRunService.compileAndRun(sourceCode);
    }
}
