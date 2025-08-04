package uk.danielgooding.koka_playground;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

public class CompileResult {
    private final boolean isSuccess;
    private final ExecutableHandle executable;
    private final String error;

    private CompileResult(boolean isSuccess, ExecutableHandle executable, String error) {
        this.isSuccess = isSuccess;
        this.executable = executable;
        this.error = error;
    }

    static CompileResult success(ExecutableHandle executable) {
        return new CompileResult(true, executable, null);
    }

    static CompileResult error(String error) {
        return new CompileResult(false, null, error);
    }

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ExecutableHandle getExecutable() {
        return executable;
    }

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getError() {
        return error;
    }

    @JsonGetter()
    public boolean getIsSuccess() {
        return isSuccess;
    }
}
