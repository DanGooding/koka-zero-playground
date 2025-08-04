package uk.danielgooding.koka_playground;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalExeHandle implements Closeable {
    private final Path path;

    LocalExeHandle(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
