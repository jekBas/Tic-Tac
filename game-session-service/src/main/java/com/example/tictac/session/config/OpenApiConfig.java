package com.example.tictac.session.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Game Session API")
						.description("Session orchestration — simulations, player-vs-computer, real-time WebSocket events")
						.version("1.0"));
	}
}