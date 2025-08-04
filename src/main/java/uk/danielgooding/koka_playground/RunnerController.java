package uk.danielgooding.koka_playground;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
