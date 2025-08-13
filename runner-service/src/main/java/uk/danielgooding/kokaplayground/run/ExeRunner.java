package uk.danielgooding.kokaplayground.run;

import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.Subprocess;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ExeRunner {

    public CompletableFuture<OrError<String>> run(Path exe, List<String> args, InputStream stdin) {
        return Subprocess.run(exe, args, stdin);
    }
}
