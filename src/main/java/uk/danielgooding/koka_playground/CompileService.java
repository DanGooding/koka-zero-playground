package uk.danielgooding.koka_playground;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CompileService {
    @Value("${compiler.exe-path}")
    private String exe_path;

    @Value("${compiler.koka-zero-config-path}")
    private String koka_zero_config_path;


    TypeCheckResult typecheck(KokaSourceCode sourceCode) {
        if (!sourceCode.getCode().contains("effect")) {
            return TypeCheckResult.invalid(String.format("missing 'effect' keyword (%s)", exe_path));
        }
        return TypeCheckResult.valid();
    }
}
