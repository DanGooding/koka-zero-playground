package uk.danielgooding.kokaplayground.protocol;

import com.fasterxml.jackson.annotation.*;
import uk.danielgooding.kokaplayground.common.exe.ExeHandle;

import java.time.Duration;

public class RunStream {

    public static class Inbound {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = Run.class, name = "run"),
                @JsonSubTypes.Type(value = Stdin.class, name = "stdin")
        })
        public static abstract sealed class Message {
            @Override
            public abstract String toString();
        }

        @JsonTypeName("run")
        public static final class Run extends Message {
            @JsonUnwrapped
            private final ExeHandle exeHandle;

            @JsonCreator
            public Run(ExeHandle handle) {
                this.exeHandle = handle;
            }

            @JsonGetter
            public ExeHandle getExeHandle() {
                return this.exeHandle;
            }

            @Override
            public String toString() {
                return "Run{" +
                        "exeHandle=" + exeHandle +
                        '}';
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

            @Override
            public String toString() {
                return "Stdin{" +
                        "content='" + content + '\'' +
                        '}';
            }
        }
    }

    public static class Outbound {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = AnotherRequestInProgress.class, name = "another-request-in-progress"),
                @JsonSubTypes.Type(value = Starting.class, name = "starting"),
                @JsonSubTypes.Type(value = Error.class, name = "error"),
                @JsonSubTypes.Type(value = Stdout.class, name = "stdout"),
                @JsonSubTypes.Type(value = Done.class, name = "done"),
                @JsonSubTypes.Type(value = Interrupted.class, name = "interrupted"),
        })
        public static abstract sealed class Message {
            @Override
            public abstract String toString();
        }

        @JsonTypeName("another-request-in-progress")
        public static final class AnotherRequestInProgress extends Message {
            @JsonCreator
            public AnotherRequestInProgress() {
            }

            @Override
            public String toString() {
                return "AnotherRequestInProgress";
            }
        }

        @JsonTypeName("starting")
        public static final class Starting extends Message {
            @JsonCreator
            public Starting() {
            }

            @Override
            public String toString() {
                return "Starting";
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

            @Override
            public String toString() {
                String escapedContent = content.replaceAll(System.lineSeparator(), "\\n");
                return "Stdout{" +
                        "content='" + escapedContent + '\'' +
                        '}';
            }
        }

        /// Error, Done, Interrupted all imply the request is complete

        /// Error is specifically a 'client error' e.g. the program segfaulted / OOMed
        /// Server errors are communicated by CloseReason at the websocket layer
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

            @Override
            public String toString() {
                return "Error{" +
                        "message='" + message + '\'' +
                        '}';
            }
        }

        @JsonTypeName("done")
        public static final class Done extends Message {
            private final Duration userWorkDuration;

            @JsonCreator
            public Done(@JsonProperty("userWorkDuration") Duration userWorkDuration) {
                this.userWorkDuration = userWorkDuration;
            }

            @JsonGetter
            public Duration getUserWorkDuration() {
                return userWorkDuration;
            }

            @Override
            public String toString() {
                return String.format("Done{userWorkDuration=%,d}", userWorkDuration.toMillis());
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

            @Override
            public String toString() {
                return "Interrupted{" +
                        "message='" + message + '\'' +
                        '}';
            }
        }
    }
}
