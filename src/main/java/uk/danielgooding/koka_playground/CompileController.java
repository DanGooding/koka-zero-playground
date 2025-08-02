package uk.danielgooding.koka_playground;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class CompileController {
    @Autowired
    private CompileService compileService;

    @PostMapping(value = "/typecheck")
    @Async
    public CompletableFuture<TypeCheckResult> typecheck(@RequestBody KokaSourceCode sourceCode) {
        return compileService.typecheck(sourceCode);
    }
}
