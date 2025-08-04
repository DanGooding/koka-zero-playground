package uk.danielgooding.koka_playground;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RunnerService {
    @Resource(name = "${which-exe-store}")
    private ExeStore exeStore;

    @Autowired
    private ExeRunner exeRunner;

    CompletableFuture<OrError<String>> runWithoutStdin(ExeHandle handle) {
        try {
            LocalExeHandle exe = exeStore.getExe(handle);

            InputStream emptyStdin = InputStream.nullInputStream();
            CompletableFuture<OrError<String>> stdout =
                    exeRunner.run(exe, List.of(), emptyStdin);
            emptyStdin.close();

            exeStore.deleteExe(handle);

            return stdout;

        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
