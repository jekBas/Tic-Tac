package com.example.tictac.session.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "session.simulation")
public record SimulationProperties(
		@NotNull
		Duration moveDelay
) {}
