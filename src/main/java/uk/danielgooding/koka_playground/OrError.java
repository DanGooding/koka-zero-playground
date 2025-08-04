package uk.danielgooding.koka_playground;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public sealed class OrError<T> {

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
}
