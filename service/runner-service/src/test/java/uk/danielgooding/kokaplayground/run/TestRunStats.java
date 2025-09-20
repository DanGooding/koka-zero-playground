package uk.danielgooding.kokaplayground.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestRunStats {
    @Test
    public void parseRunStats() throws IOException {
        String runStatsJson =
                "{ \"user_time_ns\": 583136000, \"sys_time_ns\": 1044000, \"elapsed_time_ns\": 7030390836, \"stdin_wait_time_ns\": 6447684545 }";
        ObjectMapper mapper = new ObjectMapper();

        RunStats runStats = RunStats.parseString(runStatsJson, mapper);

        assertThat(runStats.toString())
                .isEqualTo("RunStats{userTimeNS=583,136,000, sysTimeNS=1,044,000, stdinWaitTimeNS=6,447,684,545}");
    }
}