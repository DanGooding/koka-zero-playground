package uk.danielgooding.kokaplayground.run;

import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.CancellableFuture;
import uk.danielgooding.kokaplayground.common.OrError;

import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public interface IExeRunner {

    CompletableFuture<OrError<String>> runThenGetStdout(Path exe, List<String> args, String stdin);

    CancellableFuture<OrError<Void>> runStreamingStdinAndStdout(
            Path exe,
            List<String> args,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout);
}
