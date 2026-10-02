package com.oneport.notes.system;

import java.time.Instant;

public record ReleaseInfo(
		String app,
		String version,
		String gitSha,
		String builtAt,
		String color,
		String hostname,
		Instant startedAt) {
}
