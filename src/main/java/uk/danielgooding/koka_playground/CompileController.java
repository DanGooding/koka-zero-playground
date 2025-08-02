package uk.danielgooding.koka_playground;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompileController {
    @PostMapping(value = "/typecheck")
    public TypeCheckResult typecheck(@RequestBody KokaSourceCode sourceCode) {
        return sourceCode.typecheck();
    }
}
