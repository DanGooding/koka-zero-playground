package uk.danielgooding.koka_playground;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class CompileService {
    @Value("${compiler.exe-path}")
    private String exe_path;

    @Value("${compiler.koka-zero-config-path}")
    private String koka_zero_config_path;


    TypeCheckResult typecheck(KokaSourceCode sourceCode) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(exe_path, "check", "/dev/stdin");
        Process process = processBuilder.start();

        OutputStream stdin = process.getOutputStream();
        stdin.write(sourceCode.getCode().getBytes(StandardCharsets.UTF_8));
        stdin.close();

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            return TypeCheckResult.valid();
        }

        String error = new String(process.getErrorStream().readAllBytes());

        return TypeCheckResult.invalid(error);
    }
}
