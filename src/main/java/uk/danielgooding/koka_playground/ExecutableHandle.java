package uk.danielgooding.koka_playground;

import java.nio.file.Path;

public class ExecutableHandle {
    private final Path path;

    ExecutableHandle(Path path) { this.path = path; }

    public Path getPath() {
        return path;
    }
}
