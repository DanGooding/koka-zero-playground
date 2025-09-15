package uk.danielgooding.kokaplayground.compileandrun;

import java.time.Duration;

public class UserWorkStats {
    private final Duration userWorkDuration;

    public UserWorkStats(Duration userWorkDuration) {
        this.userWorkDuration = userWorkDuration;
    }

    public Duration getUserWorkDuration() {
        return userWorkDuration;
    }
}
