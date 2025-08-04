package uk.danielgooding.koka_playground;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;

public class ExeHandle {
    private final String path;

    @JsonCreator
    ExeHandle(String path) {
        this.path = path;
    }

    @JsonGetter
    public String getPath() {
        return path;
    }
}
