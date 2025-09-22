package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.Callback;
import uk.danielgooding.kokaplayground.common.CancellableFuture;
import uk.danielgooding.kokaplayground.common.OrError;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

@Service
public class SandboxedExeRunner implements IExeRunner {
    @Value("${runner.bubblewrap-path}")
    Path bubblewrapPath;

    @Autowired
    ExeRunner exeRunner;

    private List<String> addBubblewrapArgs(Path exe, List<String> exeArgs) {
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

        args.add("--");
        args.add(exe.toString());
        args.addAll(exeArgs);

        return args;
    }

    @Override
    public CompletableFuture<OrError<String>> runThenGetStdout(Path exe, List<String> exeArgs, String stdin) {
        List<String> args = addBubblewrapArgs(exe, exeArgs);
        return exeRunner.runThenGetStdout(bubblewrapPath, args, stdin);
    }

    @Override
    public CancellableFuture<OrError<Void>> runStreamingStdinAndStdout(
            Path exe,
            List<String> exeArgs,
            BlockingQueue<String> stdinBuffer,
            Callback<Void> onStart,
            Callback<String> onStdout,
            Duration realTimeLimit) {
        List<String> args = addBubblewrapArgs(exe, exeArgs);
        return exeRunner.runStreamingStdinAndStdout(
                bubblewrapPath, args, stdinBuffer, onStart, onStdout, realTimeLimit);
    }
}
