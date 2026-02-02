package com.demo.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class HaProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(HaProjectApplication.class, args);
	}

}
