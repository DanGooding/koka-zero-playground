package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;
import uk.danielgooding.kokaplayground.common.OrError;

import java.util.concurrent.CompletableFuture;

@RestController
public class RunnerController {
    @Autowired
    private RunnerService runnerService;

    @PostMapping("/run")
    public CompletableFuture<OrError<String>> run(@RequestBody ExeHandle handle) {
        return runnerService.runWithoutStdin(handle);
    }
}
