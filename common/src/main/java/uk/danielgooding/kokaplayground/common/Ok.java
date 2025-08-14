package uk.danielgooding.kokaplayground.common;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

@JsonTypeName("ok")
public final class Ok<T> extends OrError<T> {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T value;

    @JsonCreator
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
