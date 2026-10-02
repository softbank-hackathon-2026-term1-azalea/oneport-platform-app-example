package com.oneport.notes.system;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String PROPERTY_SOURCE_NAME = "databaseUrl";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String value = environment.getProperty("DATABASE_URL");
		if (!StringUtils.hasText(value)) {
			return;
		}
		environment.getPropertySources()
			.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, DatabaseUrl.parse(value).toProperties()));
	}

}
