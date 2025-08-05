package uk.danielgooding.koka_playground;

import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SandboxedExeRunner extends ExeRunner {
    @Value("${runner.bubblewrap-path}")
    String bubblewrapPath;

    @Override
    CompletableFuture<OrError<String>> run(Path exe, List<String> exeArgs, InputStream stdin) {

        List<String> args = new ArrayList<>(List.of(
                "--ro-bind", exe.toString(), exe.toString(),
                // for dynamic linking - could maybe limit to just a specific lib
                "--ro-bind", "/lib", "/lib",
                "--cap-drop", "all",
                "--unshare-all",
                "--clearenv"
        ));

        args.add("--");
        args.add(exe.toString());
        args.addAll(exeArgs);

        return super.run(Path.of(bubblewrapPath), args, stdin);
    }
}
