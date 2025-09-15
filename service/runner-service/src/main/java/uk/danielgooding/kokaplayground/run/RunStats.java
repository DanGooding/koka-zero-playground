package uk.danielgooding.kokaplayground.run;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/// Summary of a user binary run, output by instrumented koka binaries.
public class RunStats {
    private final long userTimeNS;
    private final long sysTimeNS;
    private final long stdinWaitTimeNS;

    public RunStats(long userTimeNS, long sysTimeNS, long stdinWaitTimeNS) {
        this.userTimeNS = userTimeNS;
        this.sysTimeNS = sysTimeNS;
        this.stdinWaitTimeNS = stdinWaitTimeNS;
    }

    /// Time either running user code, or blocking waiting for the user to write stdin.
    /// This constitutes the unpredictable part of the run's latency.
    /// The overhead time on top of this should be:
    /// - constant across requests
    /// - dependent on how many concurrent requests we're trying to execute
    public Duration userWorkDuration() {
        return Duration.ofNanos(userTimeNS + sysTimeNS + stdinWaitTimeNS);
    }

    public static RunStats readFile(Path path, ObjectMapper objectMapper) throws IOException {
        String content = Files.readString(path);
        return parseString(content, objectMapper);
    }

    public static RunStats parseString(String jsonString, ObjectMapper objectMapper) throws IOException {
        JsonNode node = objectMapper.readTree(jsonString);

        return new RunStats(
                node.get("user_time_ns").asLong(),
                node.get("sys_time_ns").asLong(),
                node.get("stdin_wait_time_ns").asLong());
    }

    @Override
    public String toString() {
        return String.format(
                "RunStats{userTimeNS=%,d, sysTimeNS=%,d, stdinWaitTimeNS=%,d}",
                userTimeNS, sysTimeNS, stdinWaitTimeNS);
    }
}