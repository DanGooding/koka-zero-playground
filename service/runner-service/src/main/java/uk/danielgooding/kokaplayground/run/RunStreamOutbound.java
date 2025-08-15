package uk.danielgooding.kokaplayground.run;

import com.fasterxml.jackson.annotation.*;

public class RunStreamOutbound {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = AnotherRequestInProgress.class, name = "another-request-in-progress"),
            @JsonSubTypes.Type(value = Starting.class, name = "starting"),
            @JsonSubTypes.Type(value = Error.class, name = "error"),
            @JsonSubTypes.Type(value = Stdout.class, name = "stdout"),
            @JsonSubTypes.Type(value = Done.class, name = "done"),
            @JsonSubTypes.Type(value = Interrupted.class, name = "interrupted"),
    })
    public static abstract sealed class Message {
    }

    @JsonTypeName("another-request-in-progress")
    public static final class AnotherRequestInProgress extends Message {
        @JsonCreator
        public AnotherRequestInProgress() {
        }
    }

    @JsonTypeName("starting")
    public static final class Starting extends Message {
        @JsonCreator
        public Starting() {
        }
    }

    @JsonTypeName("stdout")
    public static final class Stdout extends Message {
        private final String content;

        @JsonCreator
        public Stdout(@JsonProperty("content") String content) {
            this.content = content;
        }

        @JsonGetter
        public String getContent() {
            return content;
        }
    }

    /// Error, Done, Interrupted all imply the request is complete

    // TODO: consider distinguishing server/client errors
    @JsonTypeName("error")
    public static final class Error extends Message {
        private final String message;

        @JsonCreator
        public Error(@JsonProperty("message") String message) {
            this.message = message;
        }

        @JsonGetter
        public String getMessage() {
            return message;
        }
    }

    @JsonTypeName("done")
    public static final class Done extends Message {
        @JsonCreator
        public Done() {
        }
    }

    @JsonTypeName("interrupted")
    public static final class Interrupted extends Message {
        private final String message;

        @JsonCreator
        public Interrupted(@JsonProperty("message") String message) {
            this.message = message;
        }

        @JsonGetter
        public String getMessage() {
            return message;
        }
    }
}
