package uk.danielgooding.kokaplayground.common;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract sealed class OrError<T> permits Ok, Failed {

    public static <T> Ok<T> ok(T value) {
        return new Ok<>(value);
    }

    public static <T> OrError<T> error(String message) {
        return new Failed<>(message);
    }
}
