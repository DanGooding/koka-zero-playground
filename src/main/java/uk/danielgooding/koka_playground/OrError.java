package uk.danielgooding.koka_playground;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract sealed class OrError<T> {

    static <T> Ok<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> OrError<T> error(String message) {
        return new Failed<>(message);
    }
}

@JsonTypeName("ok")
final class Ok<T> extends OrError<T> {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T value;

    Ok(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Ok{" +
                "value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ok<?> ok = (Ok<?>) o;
        return Objects.equals(value, ok.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}

@JsonTypeName("failed")
final class Failed<T> extends OrError<T> {
    private final String message;

    Failed(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unchecked")
    <U> Failed<U> castValue() {
        return (Failed<U>) this;
    }

    @Override
    public String toString() {
        return "Failed{" +
                "message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Failed<?> failed = (Failed<?>) o;
        return Objects.equals(message, failed.message);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(message);
    }
}
