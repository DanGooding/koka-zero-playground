package uk.danielgooding.koka_playground;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KokaSourceCode {
    private final String  code;

    @JsonCreator
    public KokaSourceCode(@JsonProperty("code") String code) {
        this.code = code;
    }

    TypeCheckResult typecheck() {
        if (!code.contains("effect")) {
            return TypeCheckResult.invalid("missing 'effect' keyword");
        }
        return TypeCheckResult.valid();
    }
}
