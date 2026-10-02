package com.oneport.notes.system;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

	private static final Logger log = LoggerFactory.getLogger(HealthController.class);

	private final AppProperties properties;

	private final ReleaseInfo release;

	private final JdbcClient jdbc;

	private final @Nullable RedisConnectionFactory redis;

	HealthController(AppProperties properties, ReleaseInfo release, JdbcClient jdbc,
			ObjectProvider<RedisConnectionFactory> redis, @Value("${REDIS_URL:}") String redisUrl) {
		this.properties = properties;
		this.release = release;
		this.jdbc = jdbc;
		this.redis = StringUtils.hasText(redisUrl) ? redis.getIfAvailable() : null;
	}

	@GetMapping("/health")
	ResponseEntity<Map<String, String>> health() {
		if (properties.forceUnhealthy()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("status", "unhealthy", "reason", "APP_FORCE_UNHEALTHY"));
		}
		return ResponseEntity.ok(Map.of("status", "ok"));
	}

	@GetMapping("/ready")
	ResponseEntity<Map<String, String>> ready() {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("status", "ok");
		body.put("database", databaseStatus());
		body.put("cache", cacheStatus());
		if (body.containsValue("down")) {
			body.put("status", "unavailable");
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
		}
		return ResponseEntity.ok(body);
	}

	private String databaseStatus() {
		try {
			jdbc.sql("SELECT 1").query(Integer.class).single();
			return "up";
		}
		catch (DataAccessException ex) {
			log.warn("database readiness check failed", ex);
			return "down";
		}
	}

	private String cacheStatus() {
		if (redis == null) {
			return "disabled";
		}
		try (RedisConnection connection = redis.getConnection()) {
			connection.ping();
			return "up";
		}
		catch (DataAccessException ex) {
			log.warn("cache readiness check failed", ex);
			return "down";
		}
	}

	@GetMapping("/version")
	ReleaseInfo version() {
		return release;
	}

	@GetMapping("/failure")
	ResponseEntity<Map<String, String>> failure() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failure"));
	}

}
