package com.oneport.notes.system;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

record DatabaseUrl(String jdbcUrl, String username, String password) {

	private static final Set<String> SCHEMES = Set.of("postgres", "postgresql");

	static DatabaseUrl parse(String value) {
		URI uri = URI.create(value);
		if (!SCHEMES.contains(uri.getScheme())) {
			throw new IllegalArgumentException("DATABASE_URL must use the postgresql:// scheme");
		}
		if (uri.getHost() == null || uri.getPath() == null || uri.getPath().length() <= 1) {
			throw new IllegalArgumentException("DATABASE_URL must include a host and a database name");
		}
		String username = null;
		String password = null;
		String userInfo = uri.getRawUserInfo();
		if (userInfo != null) {
			int colon = userInfo.indexOf(':');
			username = decode((colon < 0) ? userInfo : userInfo.substring(0, colon));
			password = (colon < 0) ? null : decode(userInfo.substring(colon + 1));
		}
		int port = (uri.getPort() > 0) ? uri.getPort() : 5432;
		String query = (uri.getRawQuery() != null) ? "?" + uri.getRawQuery() : "";
		String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query;
		return new DatabaseUrl(jdbcUrl, username, password);
	}

	Map<String, Object> toProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("spring.datasource.url", jdbcUrl);
		if (username != null) {
			properties.put("spring.datasource.username", username);
		}
		if (password != null) {
			properties.put("spring.datasource.password", password);
		}
		return properties;
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

}
