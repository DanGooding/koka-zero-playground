package uk.danielgooding.koka_playground;

import org.springframework.stereotype.Service;
import uk.danielgooding.koka_playground.common.OrError;
import uk.danielgooding.koka_playground.common.Subprocess;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ExeRunner {
    ExeRunner() {
    }

    CompletableFuture<OrError<String>> run(Path exe, List<String> args, InputStream stdin) {
        return Subprocess.run(exe, args, stdin);
    }
}
