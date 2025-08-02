package uk.danielgooding.koka_playground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KokaPlaygroundApplication {

	public static void main(String[] args) {
		SpringApplication.run(KokaPlaygroundApplication.class, args);
	}

}
