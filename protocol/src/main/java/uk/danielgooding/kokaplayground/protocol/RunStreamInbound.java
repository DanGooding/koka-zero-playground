package uk.danielgooding.kokaplayground.protocol;

import com.fasterxml.jackson.annotation.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;

public class RunStreamInbound {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Run.class, name = "run"),
            @JsonSubTypes.Type(value = Stdin.class, name = "stdin")
    })
    public static abstract sealed class Message {
    }

    @JsonTypeName("run")
    public static final class Run extends Message {
        private final ExeHandle exeHandle;

        @JsonCreator
        public Run(@JsonProperty("exeHandle") ExeHandle handle) {
            this.exeHandle = handle;
        }

        @JsonGetter
        public ExeHandle getExeHandle() {
            return this.exeHandle;
        }
    }

    @JsonTypeName("stdin")
    public static final class Stdin extends Message {
        private final String content;

        @JsonCreator
        public Stdin(@JsonProperty("content") String content) {
            this.content = content;
        }

        @JsonGetter
        public String getContent() {
            return content;
        }
    }
}
