package com.oneport.notes.system;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

	private static final Logger log = LoggerFactory.getLogger(HealthController.class);

	private final AppProperties properties;

	private final ReleaseInfo release;

	private final JdbcClient jdbc;

	HealthController(AppProperties properties, ReleaseInfo release, JdbcClient jdbc) {
		this.properties = properties;
		this.release = release;
		this.jdbc = jdbc;
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
		try {
			jdbc.sql("SELECT 1").query(Integer.class).single();
		}
		catch (DataAccessException ex) {
			log.warn("readiness check failed", ex);
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("status", "unavailable", "database", "down"));
		}
		return ResponseEntity.ok(Map.of("status", "ok", "database", "up"));
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
