package uk.danielgooding.kokaplayground.common;

public record ExitCode(int code) {

    public boolean isSuccess() {
        return code == 0;
    }

    public String errorMessage() {
        // based on https://tldp.org/LDP/abs/html/exitcodes.html
        return switch (code) {
            case 126 -> "Not executable";
            case 127 -> "Command not found";
            case 128 -> "Exited with invalid code";
            // `128 + n` indicates exiting due to signal `n`.
            // Note some signal numbers are architecture specific
            default -> switch (code - 128) {
                case 2 -> "Interrupted"; // SIGINT
                case 6 -> "Aborted"; // SIGABRT
                case 7 -> "Bus error"; // SIGBUS
                case 9 -> "Forcibly killed"; // SIGKILL
                case 11 -> "Segmentation fault"; // SIGSEGV
                case 15 -> "Killed"; // SIGTERM
                default -> String.format("Exit [%d]", code);
            };
        };
    }
}
