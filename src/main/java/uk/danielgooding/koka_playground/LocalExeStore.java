package uk.danielgooding.koka_playground;


import com.fasterxml.jackson.annotation.JsonGetter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service("local-exe-store")
class LocalExeStore implements ExeStore<LocalExeStore.Handle> {
    private final Path directory;
    private int nextId = 0;

    LocalExeStore() throws IOException {
        directory = Files.createTempDirectory("exe-store");
    }

    public static class Handle implements ExeHandle {
        private final Path path;

        Handle(Path path) {
            this.path = path;
        }

        @JsonGetter
        public Path getPath() {
            return path;
        }
    }

    private Handle freshHandle() {
        Path path = directory.resolve(String.format("exe-%d", nextId++));
        return new Handle(path);
    }

    @Override
    public Handle putExe(Path src) throws IOException {
        Handle handle = freshHandle();
        Files.copy(src, handle.path);
        return handle;
    }

    @Override
    public void deleteExe(Handle handle) throws IOException {
        Files.deleteIfExists(handle.path);
    }

    @Override
    public Path getExe(Handle handle) {
        return handle.path;
    }

}
