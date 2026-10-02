package com.oneport.notes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "app.force-unhealthy=true", "LOG_FORMAT=text" })
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
class ForcedUnhealthyTests {

	@Autowired
	private RestTestClient client;

	@Test
	void healthReturns503WhenForced() {
		client.get()
			.uri("/health")
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
			.expectBody()
			.jsonPath("$.reason")
			.isEqualTo("APP_FORCE_UNHEALTHY");
	}

	@Test
	void readyStillReportsDatabase() {
		client.get().uri("/ready").exchange().expectStatus().isOk();
	}

}
