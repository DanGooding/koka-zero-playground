package uk.danielgooding.koka_playground;

import java.io.IOException;
import java.nio.file.Path;

public interface ExeStore {

    ExeHandle putExe(Path src) throws IOException;

    OrError<Path> getExe(ExeHandle handle, Workdir workdir) throws IOException;

    void deleteExe(ExeHandle handle) throws IOException;
}
