package com.oneport.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NotesApplication {

	public static void main(String[] args) {

		String logFormat = System.getenv().getOrDefault("LOG_FORMAT", "json");
		if (!"text".equalsIgnoreCase(logFormat)) {
			System.setProperty("logging.structured.format.console", "ecs");
		}
		SpringApplication.run(NotesApplication.class, args);
	}

}
