package uk.danielgooding.koka_playground;

import java.io.IOException;

public interface ExeStore {

    ExeHandle putExe(LocalExeHandle src) throws IOException;

    OrError<LocalExeHandle> getExe(ExeHandle handle, Workdir workdir) throws IOException;

    void deleteExe(ExeHandle handle) throws IOException;
}
