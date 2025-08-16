package uk.danielgooding.kokaplayground.common.exe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExeHandle {
    private final String path;

    @JsonCreator
    public ExeHandle(@JsonProperty("path") String path) {
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
