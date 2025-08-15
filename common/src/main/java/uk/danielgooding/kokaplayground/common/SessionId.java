package uk.danielgooding.kokaplayground.common;

import org.springframework.lang.NonNull;

import java.util.Objects;

public record SessionId(String id) {
    @Override
    public @NonNull String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SessionId sessionId = (SessionId) o;
        return Objects.equals(id, sessionId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
