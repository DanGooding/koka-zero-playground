package uk.danielgooding.koka_playground;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KokaSourceCode {
    private final String  code;

    @JsonCreator
    public KokaSourceCode(@JsonProperty("code") String code) {
        this.code = code;
    }

    String getCode() {
        return code;
    }
}
