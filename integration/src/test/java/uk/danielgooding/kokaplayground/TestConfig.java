package uk.danielgooding.kokaplayground;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@ComponentScan(basePackages = {
        "uk.danielgooding.kokaplayground"
})
public class TestConfig {
}
