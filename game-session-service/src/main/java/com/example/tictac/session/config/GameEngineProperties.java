package com.example.tictac.session.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the Game Engine Service client.
 * <p>Backed by the {@code game-engine.*} properties.
 */
@Validated
@ConfigurationProperties(prefix = "game-engine")
public record GameEngineProperties(
		@NotBlank String baseUrl,
		@NotNull Duration connectTimeout,
		@NotNull Duration readTimeout
) {

	public GameEngineProperties {
		if (connectTimeout == null) {
			connectTimeout = Duration.ofSeconds(2);
		}
		if (readTimeout == null) {
			readTimeout = Duration.ofSeconds(5);
		}
	}
}
