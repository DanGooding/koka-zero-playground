package uk.danielgooding.kokaplayground.common;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CancellableFuture<T> {

    private final CompletableFuture<OrCancelled<T>> resultFuture;
    private final Canceler canceler;

    public CancellableFuture(CompletableFuture<OrCancelled<T>> result, Canceler canceler) {
        this.resultFuture = result;
        this.canceler = canceler;
    }

    public void cancel() {
        this.canceler.cancel();
    }

    public static <T> CancellableFuture<T> completedFuture(T result) {
        return new CancellableFuture<>(
                CompletableFuture.completedFuture(OrCancelled.ok(result)),
                new Canceler()
        );
    }

    public static <T> CancellableFuture<T> failedFuture(Throwable exn) {
        return new CancellableFuture<>(
                CompletableFuture.failedFuture(exn),
                new Canceler()
        );
    }

    /// acts like `CompletableFuture.supplyAsync(run)` but `run` receives a canceller
    /// which it is expected to populate
    public static <T> CancellableFuture<T> supplyAsync(Function<Canceler, OrCancelled<T>> run) {
        Canceler canceler = new Canceler();
        CompletableFuture<OrCancelled<T>> result = CompletableFuture.supplyAsync(() ->
                canceler.wrapIfCancelled(run.apply(canceler)));
        return new CancellableFuture<>(result, canceler);
    }

    public <U> CancellableFuture<U> thenApply(Function<T, U> fn) {

        CompletableFuture<OrCancelled<U>> resultFuture =
                this.resultFuture.thenApply((result) ->
                        canceler.wrapIfCancelled(result.thenApply(fn)));
        return new CancellableFuture<>(resultFuture, canceler);
    }

    public CancellableFuture<Void> thenAccept(Consumer<T> action) {
        return thenApply(result -> {
            action.accept(result);
            return null;
        });
    }

    public CancellableFuture<T> whenComplete(BiConsumer<OrCancelled<T>, Throwable> callback) {
        return new CancellableFuture<>(
                resultFuture.whenComplete(callback),
                canceler
        );
    }

    /// true if completed, failed, or cancelled
    public boolean isDone() {
        return resultFuture.isDone();
    }

}
