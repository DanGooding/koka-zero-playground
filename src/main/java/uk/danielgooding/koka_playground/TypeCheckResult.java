package uk.danielgooding.koka_playground;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

public class TypeCheckResult {
    private final boolean valid;
    private final String error;

    @JsonGetter
    boolean getValid() {
        return this.valid;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonGetter
    String getError() {
        return this.error;
    }

    private TypeCheckResult(boolean valid, String error) {
        this.valid = valid;
        this.error = error;
    }

    static TypeCheckResult valid() {
        return new TypeCheckResult(true, null);
    }
    static TypeCheckResult invalid(String error) {
        return new TypeCheckResult(false, error);
    }
}
