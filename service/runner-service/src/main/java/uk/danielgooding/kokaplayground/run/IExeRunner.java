package uk.danielgooding.kokaplayground.run;

import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.OrError;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IExeRunner {

    CompletableFuture<OrError<String>> runThenGetStdout(Path exe, List<String> args, String stdin);

    CompletableFuture<OrError<Void>> runStreamingStdout(
            Path exe,
            List<String> args,
            String stdin,
            Callback<Void> onStart,
            Callback<String> onStdout);
}
