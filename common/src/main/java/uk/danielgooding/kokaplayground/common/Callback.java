package uk.danielgooding.kokaplayground.common;

public interface Callback<T> {
    void call(T arg) throws Exception;
}
