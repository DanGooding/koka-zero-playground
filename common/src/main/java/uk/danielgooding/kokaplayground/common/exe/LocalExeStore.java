package uk.danielgooding.kokaplayground.common.exe;


import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.Workdir;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

@Service
@Lazy
public class LocalExeStore implements ExeStore {
    private final Path directory;
    private int nextId = 0;

    public LocalExeStore(@Value("${local-exe-store.directory}") Path inDirectory) throws IOException {
        Files.createDirectories(inDirectory);
        directory = Files.createTempDirectory(inDirectory, "exe-store");
    }

    private synchronized ExeHandle freshHandle() {
        Path path = directory.resolve(String.format("exe-%d", nextId++));
        return new ExeHandle(path.toString());
    }

    @Override
    public ExeHandle putExe(byte[] exe) throws IOException {
        ExeHandle handle = freshHandle();
        Path path = Path.of(handle.getPath());
        Files.write(path, exe,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        Files.setPosixFilePermissions(path,
                EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE));

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

    @PreDestroy
    public void cleanup() throws IOException {
        FileSystemUtils.deleteRecursively(directory);
    }

}
