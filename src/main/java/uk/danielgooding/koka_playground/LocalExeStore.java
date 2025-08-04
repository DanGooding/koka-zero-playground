package uk.danielgooding.koka_playground;


import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service("local-exe-store")
class LocalExeStore implements ExeStore {
    private final Path directory;
    private int nextId = 0;

    LocalExeStore() throws IOException {
        directory = Files.createTempDirectory("exe-store");
    }

    private ExeHandle freshHandle() {
        Path path = directory.resolve(String.format("exe-%d", nextId++));
        return new ExeHandle(path.toString());
    }

    @Override
    public ExeHandle putExe(LocalExeHandle src) throws IOException {
        ExeHandle handle = freshHandle();
        Files.copy(src.getPath(), Path.of(handle.getPath()));
        return handle;
    }

    @Override
    public void deleteExe(ExeHandle handle) throws IOException {
        Files.deleteIfExists(Path.of(handle.getPath()));
    }

    @Override
    public OrError<LocalExeHandle> getExe(ExeHandle handle) {
        Path path = Path.of(handle.getPath());
        if (Files.exists(path)) {
            return OrError.ok(new LocalExeHandle(path));
        } else {
            return OrError.error(String.format("exe not found: %s", handle.getPath()));
        }
    }

}
