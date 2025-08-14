package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import uk.danielgooding.kokaplayground.common.ExeHandle;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;
import uk.danielgooding.kokaplayground.common.OrError;

import java.lang.reflect.Type;
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
