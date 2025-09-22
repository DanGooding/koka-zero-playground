package uk.danielgooding.kokaplayground.common;

import java.util.Objects;
import java.util.function.Function;

public abstract sealed class OrInterrupted<T> {
    public abstract <U> OrInterrupted<U> thenApply(Function<T, U> fn);

    public static <T> Ok<T> ok(T t) {
        return new Ok<>(t);
    }

    public static <T> Interrupted<T> interrupted() {
        return new Interrupted<>();
    }

    public abstract <U> OrInterrupted<U> map(Function<T, U> f);

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    public static final class Ok<T> extends OrInterrupted<T> {
        private final T result;

        public Ok(T result) {
            this.result = result;
        }

        public T getResult() {
            return result;
        }

        @Override
        public <U> OrInterrupted<U> thenApply(Function<T, U> fn) {
            return OrInterrupted.ok(fn.apply(result));
        }

        @Override
        public <U> OrInterrupted<U> map(Function<T, U> f) {
            return new Ok<>(f.apply(result));
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

    public static final class Interrupted<T> extends OrInterrupted<T> {
        @Override
        public <U> OrInterrupted<U> thenApply(Function<T, U> fn) {
            return OrInterrupted.interrupted();
        }

        @Override
        public <U> OrInterrupted<U> map(Function<T, U> f) {
            return OrInterrupted.interrupted();
        }

        @Override
        public String toString() {
            return "Interrupted";
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
