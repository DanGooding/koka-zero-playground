package uk.danielgooding.kokaplayground.compile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.danielgooding.kokaplayground.common.ExeHandle;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.CompletableFuture;

@RestController
public class CompileController {
    @Autowired
    private CompileService compileService;

    @PostMapping(value = "/typecheck")
    public CompletableFuture<OrError<Void>> typecheck(@RequestBody KokaSourceCode sourceCode) {
        return compileService.typecheck(sourceCode);
    }

    @PostMapping(value = "/compile")
    public CompletableFuture<OrError<ExeHandle>> compile(@RequestBody KokaSourceCode sourceCode) {
        return compileService.compile(sourceCode, true);
    }
}
