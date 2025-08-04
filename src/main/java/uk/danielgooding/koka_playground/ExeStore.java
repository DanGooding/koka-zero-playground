package uk.danielgooding.koka_playground;

import java.io.IOException;
import java.nio.file.Path;

public interface ExeStore<Handle extends ExeHandle> {

    Handle putExe(Path src) throws IOException;

    Path getExe(Handle handle) throws IOException;

    void deleteExe(Handle handle) throws IOException;
}
