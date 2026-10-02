package com.oneport.notes.system;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReleaseConfiguration {

	@Bean
	ReleaseInfo releaseInfo(AppProperties properties, ObjectProvider<BuildProperties> buildProperties) {
		BuildProperties build = buildProperties.getIfAvailable();
		String version = (build != null) ? build.getVersion() : "dev";
		String builtAt = (build != null && build.getTime() != null)
				? build.getTime().truncatedTo(ChronoUnit.SECONDS).toString() : "unknown";
		return new ReleaseInfo(properties.name(), version, properties.release().gitSha(),
				builtAt, properties.color(), hostname(),
				Instant.now().truncatedTo(ChronoUnit.SECONDS));
	}

	private static String hostname() {
		String fromEnv = System.getenv("HOSTNAME");
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv;
		}
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException ex) {
			return "unknown";
		}
	}

}
