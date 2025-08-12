package uk.danielgooding.koka_playground.run;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.danielgooding.koka_playground.common.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RunnerService {
    @Resource(name = "${which-exe-store}")
    private ExeStore exeStore;

    @Autowired
    private ExeRunner exeRunner;

    @Autowired
    @Qualifier("runner-workdir")
    private Workdir workdir;

    CompletableFuture<OrError<String>> runWithoutStdin(ExeHandle handle) {
        try {
            Path exe;
            switch (exeStore.getExe(handle, workdir)) {
                case Failed<?> failed -> {
                    return CompletableFuture.completedFuture(failed.castValue());
                }
                case Ok<Path> okExe -> {
                    exe = okExe.getValue();
                }
            }


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
