package uk.danielgooding.kokaplayground.run;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "uk.danielgooding.kokaplayground.common",
        "uk.danielgooding.kokaplayground.run",
})
public class RunnerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RunnerServiceApplication.class, args);
    }

}
