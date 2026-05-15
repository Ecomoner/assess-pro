package com.frist.assesspro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AssessProApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssessProApplication.class, args);
	}

}
