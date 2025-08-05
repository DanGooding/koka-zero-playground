package uk.danielgooding.koka_playground;


import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public ExeHandle putExe(Path src) throws IOException {
        ExeHandle handle = freshHandle();
        Files.copy(src, Path.of(handle.getPath()));
        return handle;
    }

    @Override
    public void deleteExe(ExeHandle handle) throws IOException {
        Files.deleteIfExists(Path.of(handle.getPath()));
    }

    boolean contains(ExeHandle handle) {
        Path path = Path.of(handle.getPath());
        return Files.exists(path);
    }

    @Override
    public OrError<Path> getExe(ExeHandle handle, Workdir workdir) throws IOException {
        if (!contains(handle)) {
            return OrError.error(String.format("exe not found: %s", handle.getPath()));
        }
        Path destination = workdir.freshPath("downloaded");
        Files.copy(Path.of(handle.getPath()), destination);
        return OrError.ok(destination);
    }

}
