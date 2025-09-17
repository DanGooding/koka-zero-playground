package uk.danielgooding.kokaplayground.common.exe;

import uk.danielgooding.kokaplayground.common.OrError;
import uk.danielgooding.kokaplayground.common.Workdir;

import java.io.IOException;
import java.nio.file.Path;

public interface ExeStore {

    ExeHandle putExe(byte[] exe) throws IOException;

    OrError<Path> getExe(ExeHandle handle, Workdir workdir) throws IOException;

    void deleteExe(ExeHandle handle) throws IOException;
}
