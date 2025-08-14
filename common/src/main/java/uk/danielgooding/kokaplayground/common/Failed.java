package uk.danielgooding.kokaplayground.common;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

@JsonTypeName("failed")
public final class Failed<T> extends OrError<T> {
    private final String message;

    @JsonCreator
    Failed(@JsonProperty("message") String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unchecked")
    public <U> Failed<U> castValue() {
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
