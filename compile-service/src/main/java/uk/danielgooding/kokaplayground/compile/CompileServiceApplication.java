package uk.danielgooding.kokaplayground.compile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "uk.danielgooding.kokaplayground.common",
        "uk.danielgooding.kokaplayground.compile"
})
public class CompileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompileServiceApplication.class, args);
    }

}
