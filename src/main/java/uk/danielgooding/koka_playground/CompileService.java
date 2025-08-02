package uk.danielgooding.koka_playground;

import org.springframework.stereotype.Service;

@Service
public class CompileService {
    TypeCheckResult typecheck(KokaSourceCode sourceCode) {
        if (!sourceCode.getCode().contains("effect")) {
            return TypeCheckResult.invalid("missing 'effect' keyword");
        }
        return TypeCheckResult.valid();
    }
}
