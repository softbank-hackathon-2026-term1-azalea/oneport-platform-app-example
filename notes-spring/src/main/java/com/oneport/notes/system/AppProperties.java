package com.oneport.notes.system;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app")
@Validated
public record AppProperties(@NotBlank String name, @NotBlank String color, boolean forceUnhealthy,
		@Valid Release release) {

	public record Release(@NotBlank String version, @NotBlank String gitSha, @NotBlank String builtAt) {
	}

}
