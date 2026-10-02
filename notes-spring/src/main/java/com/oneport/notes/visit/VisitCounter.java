package com.oneport.notes.visit;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class VisitCounter {

	static final String KEY = "notes:visits";

	private final @Nullable StringRedisTemplate redis;

	VisitCounter(ObjectProvider<StringRedisTemplate> redis, @Value("${REDIS_URL:}") String redisUrl) {
		this.redis = StringUtils.hasText(redisUrl) ? redis.getIfAvailable() : null;
	}

	long record() {
		return Objects.requireNonNull(values().increment(KEY));
	}

	long count() {
		String value = values().get(KEY);
		return value == null ? 0 : Long.parseLong(value);
	}

	private ValueOperations<String, String> values() {
		if (redis == null) {
			throw new CacheNotConfiguredException();
		}
		return redis.opsForValue();
	}

}
