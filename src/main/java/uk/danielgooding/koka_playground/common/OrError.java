package uk.danielgooding.koka_playground.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract sealed class OrError<T> permits Ok, Failed {

    public static <T> Ok<T> ok(T value) {
        return new Ok<>(value);
    }

    public static <T> OrError<T> error(String message) {
        return new Failed<>(message);
    }
}
