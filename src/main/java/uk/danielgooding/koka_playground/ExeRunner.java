package uk.danielgooding.koka_playground;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ExeRunner {
    ExeRunner() {
    }

    CompletableFuture<OrError<String>> run(LocalExeHandle exe, List<String> args, InputStream stdin) {
        return Subprocess.run(exe.getPath(), args, stdin);
    }
}
