package com.oneport.notes;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "REDIS_URL=redis://127.0.0.1:1/0", "LOG_FORMAT=text" })
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
class UnreachableCacheTests {

	@Autowired
	private RestTestClient client;

	@Test
	void visitsReturn503() {
		client.post()
			.uri("/visits")
			.exchange()
			.expectStatus()
			.isEqualTo(503)
			.expectBody()
			.jsonPath("$.detail")
			.isEqualTo("cache unavailable");
	}

	@Test
	void readyReportsCacheDown() {
		client.get()
			.uri("/ready")
			.exchange()
			.expectStatus()
			.isEqualTo(503)
			.expectBody(Map.class)
			.isEqualTo(Map.of("status", "unavailable", "database", "up", "cache", "down"));
	}

}
