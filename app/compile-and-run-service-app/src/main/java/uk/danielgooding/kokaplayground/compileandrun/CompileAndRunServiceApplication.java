package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "uk.danielgooding.kokaplayground.common",
        "uk.danielgooding.kokaplayground.compileandrun"
})
public class CompileAndRunServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompileAndRunServiceApplication.class, args);
    }

}
