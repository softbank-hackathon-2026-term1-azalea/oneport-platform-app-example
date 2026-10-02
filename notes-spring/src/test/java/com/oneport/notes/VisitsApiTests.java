package com.oneport.notes;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "LOG_FORMAT=text")
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@Testcontainers
class VisitsApiTests {

	@Container
	static final GenericContainer<?> valkey = new GenericContainer<>(DockerImageName.parse("valkey/valkey:8-alpine"))
		.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisUrl(DynamicPropertyRegistry registry) {
		registry.add("REDIS_URL", () -> "redis://%s:%d/0".formatted(valkey.getHost(), valkey.getMappedPort(6379)));
	}

	@Autowired
	private RestTestClient client;

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void resetCounter() {
		redis.delete("notes:visits");
	}

	@Test
	void visitsStartAtZero() {
		client.get().uri("/visits").exchange().expectStatus().isOk().expectBody().json("{\"visits\":0}");
	}

	@Test
	void recordingAVisitIncrementsTheCounter() {
		client.post().uri("/visits").exchange().expectStatus().isOk().expectBody().json("{\"visits\":1}");
		client.post().uri("/visits").exchange().expectStatus().isOk().expectBody().json("{\"visits\":2}");
		client.get().uri("/visits").exchange().expectStatus().isOk().expectBody().json("{\"visits\":2}");
		assertThat(redis.opsForValue().get("notes:visits")).isEqualTo("2");
	}

	@Test
	void readyReportsCache() {
		client.get()
			.uri("/ready")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Map.class)
			.isEqualTo(Map.of("status", "ok", "database", "up", "cache", "up"));
	}

}
