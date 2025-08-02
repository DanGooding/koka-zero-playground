package uk.danielgooding.koka_playground;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.Future;

@RestController
public class CompileController {
    @Autowired
    private CompileService compileService;

    @PostMapping(value = "/typecheck")
    @Async
    public Future<TypeCheckResult> typecheck(@RequestBody KokaSourceCode sourceCode) throws IOException {
        return compileService.typecheck(sourceCode);
    }
}
