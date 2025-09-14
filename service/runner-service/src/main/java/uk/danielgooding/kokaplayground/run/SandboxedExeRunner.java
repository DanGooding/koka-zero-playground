package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.CancellableFuture;
import uk.danielgooding.kokaplayground.common.OrError;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

@Service
public class SandboxedExeRunner implements IExeRunner {
    @Value("${runner.bubblewrap-path}")
    Path bubblewrapPath;

    @Autowired
    ExeRunner exeRunner;

    private List<String> addBubblewrapArgs(Path exe, List<String> exeArgs, Map<String, String> environment) {
        List<String> args = new ArrayList<>(List.of(
                "--ro-bind", exe.toString(), exe.toString(),
                // for dynamic linking - could maybe limit to just a specific lib
                "--ro-bind", "/lib", "/lib",
                "--ro-bind", "/usr/local/lib", "/usr/local/lib",
                // GC reads /proc/stat, presumably to find system memory / num cpus
                "--ro-bind", "/proc/stat", "/proc/stat",
                "--cap-drop", "all",
                // run in a new namespace for every resource
                "--unshare-all",
                "--clearenv",
                // https://github.com/containers/bubblewrap?tab=readme-ov-file#limitations
                "--new-session",
                "--die-with-parent"
        ));

        for (Map.Entry<String, String> binding : environment.entrySet()) {
            args.addAll(List.of("--setenv", binding.getKey(), binding.getValue()));
        }

        args.add("--");
        args.add(exe.toString());
        args.addAll(exeArgs);

        return args;
    }

    public CompletableFuture<OrError<String>> runThenGetStdout(Path exe, List<String> exeArgs, Map<String, String> environement, String stdin) {
        List<String> args = addBubblewrapArgs(exe, exeArgs, environement);
        return exeRunner.runThenGetStdout(bubblewrapPath, args, Map.of(), stdin);
    }

    public CancellableFuture<OrError<Void>> runStreamingStdinAndStdout(
            Path exe,
            List<String> exeArgs,
            Map<String, String> environment,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout
    ) {
        List<String> args = addBubblewrapArgs(exe, exeArgs, environment);
        return exeRunner.runStreamingStdinAndStdout(bubblewrapPath, args, Map.of(), stdinBuffer, onStart, onStdout);
    }
}
