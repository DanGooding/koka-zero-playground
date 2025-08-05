package uk.danielgooding.koka_playground;

import java.nio.file.Path;
import java.util.Objects;

public class LocalExeHandle {
    private final Path path;

    LocalExeHandle(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "LocalExeHandle{" +
                "path=" + path +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LocalExeHandle that = (LocalExeHandle) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }
}
