package uk.danielgooding.kokaplayground.common;

import java.io.IOException;

public interface Callback<T> {
    void call(T arg) throws IOException;
}
