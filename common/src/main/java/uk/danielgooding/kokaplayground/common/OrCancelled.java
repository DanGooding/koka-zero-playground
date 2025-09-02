package uk.danielgooding.kokaplayground.common;

import java.util.Objects;
import java.util.function.Function;

public abstract sealed class OrCancelled<T> {
    public abstract <U> OrCancelled<U> thenApply(Function<T, U> fn);

    public static <T> Ok<T> ok(T t) {
        return new Ok<>(t);
    }

    public static <T> Cancelled<T> cancelled() {
        return new Cancelled<>();
    }

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    public static final class Ok<T> extends OrCancelled<T> {
        private final T result;

        public Ok(T result) {
            this.result = result;
        }

        public T getResult() {
            return result;
        }

        @Override
        public <U> OrCancelled<U> thenApply(Function<T, U> fn) {
            return OrCancelled.ok(fn.apply(result));
        }

        @Override
        public String toString() {
            return "Ok{" +
                    "result=" + result +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Ok<?> ok = (Ok<?>) o;
            return Objects.equals(result, ok.result);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(result);
        }
    }

    public static final class Cancelled<T> extends OrCancelled<T> {
        @Override
        public <U> OrCancelled<U> thenApply(Function<T, U> fn) {
            return OrCancelled.cancelled();
        }

        @Override
        public String toString() {
            return "Cancelled";
        }

        @Override
        public boolean equals(Object o) {
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(null);
        }
    }

}
