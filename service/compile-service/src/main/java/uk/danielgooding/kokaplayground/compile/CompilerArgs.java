package uk.danielgooding.kokaplayground.compile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CompilerArgs {
    @Value("${compiler.args.optimise}")
    private boolean shouldOptimise;

    @Value("${compiler.args.enable-run-stats}")
    private boolean enableRunStats;

    public boolean shouldOptimise() {
        return this.shouldOptimise;
    }

    public boolean enableRunStats() {
        return this.enableRunStats;
    }
}
