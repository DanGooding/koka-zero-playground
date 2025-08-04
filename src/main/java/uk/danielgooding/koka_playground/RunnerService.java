package uk.danielgooding.koka_playground;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RunnerService {
    @Resource(name = "${which-exe-store}")
    private ExeStore exeStore;

    CompletableFuture<OrError<String>> runWithoutStdin(ExeHandle handle) {
        try {
            Path exePath = exeStore.getExe(handle);

            InputStream emptyStdin = InputStream.nullInputStream();

            CompletableFuture<OrError<String>> stdout =
                    Subprocess.run(exePath.toString(), List.of(), emptyStdin);
            emptyStdin.close();

            exeStore.deleteExe(handle);

            return stdout;

        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
