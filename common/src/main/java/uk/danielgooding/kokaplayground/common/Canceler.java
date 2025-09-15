package uk.danielgooding.kokaplayground.common;

import org.springframework.lang.Nullable;

/// This allows a main thread to spin off a worker thread, and then ask it to cancel itself.
/// Whatever cancellation code the worker registers with `setOnCancel` will be run when the
/// main thread calls `cancel`
public class Canceler {
    private boolean isCancelled = false;
    private @Nullable Runnable onCancel = null;

    public synchronized void cancel() {
        if (isCancelled) return; // already cancelled
        isCancelled = true;

        if (onCancel != null) {
            onCancel.run();
            onCancel = null;
        }
    }

    public synchronized void setOnCancel(Runnable cancel) throws CancelledException {
        if (isCancelled) { // cancelled before we attached this callback - run it now
            cancel();
            throw new CancelledException();
        }
        this.onCancel = cancel;
    }

    public synchronized <T> OrCancelled<T> wrapIfCancelled(OrCancelled<T> result) {
        if (result instanceof OrCancelled.Ok<T> && isCancelled) {
            // must have already run onCancel if it was set
            // so no need to run again
            return OrCancelled.cancelled();
        }
        return result;
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}
