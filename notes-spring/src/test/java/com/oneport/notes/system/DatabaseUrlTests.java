package com.oneport.notes.system;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DatabaseUrlTests {

	@Test
	void parsesPlatformDatabaseUrl() {
		DatabaseUrl url = DatabaseUrl
			.parse("postgresql://app_admin:p%40ss-_w0rd@oneport-db-notes.example.rds.amazonaws.com:5432/notes_java?sslmode=require");

		assertThat(url.jdbcUrl())
			.isEqualTo("jdbc:postgresql://oneport-db-notes.example.rds.amazonaws.com:5432/notes_java?sslmode=require");
		assertThat(url.username()).isEqualTo("app_admin");
		assertThat(url.password()).isEqualTo("p@ss-_w0rd");
	}

	@Test
	void defaultsPortAndAcceptsPostgresScheme() {
		assertThat(DatabaseUrl.parse("postgres://u:p@db/notes").jdbcUrl()).isEqualTo("jdbc:postgresql://db:5432/notes");
	}

	@Test
	void rejectsOtherSchemesAndMissingDatabase() {
		assertThatIllegalArgumentException().isThrownBy(() -> DatabaseUrl.parse("mysql://u:p@db/notes"));
		assertThatIllegalArgumentException().isThrownBy(() -> DatabaseUrl.parse("postgresql://u:p@db"));
	}

	@Test
	void postProcessorOverridesDatasourceProperties() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("DATABASE_URL", "postgresql://u:p@db:6543/notes")
			.withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/notes");

		new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null);

		assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://db:6543/notes");
		assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("u");
		assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("p");
	}

	@Test
	void postProcessorDoesNothingWithoutDatabaseUrl() {
		MockEnvironment environment = new MockEnvironment();
		new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null);
		assertThat(environment.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
			.isFalse();
	}

}
