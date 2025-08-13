package uk.danielgooding.kokaplayground.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;

public class ExeHandle {
    private final String path;

    @JsonCreator
    public ExeHandle(String path) {
        this.path = path;
    }

    @JsonGetter
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "ExeHandle{" +
                "path='" + path + '\'' +
                '}';
    }
}
