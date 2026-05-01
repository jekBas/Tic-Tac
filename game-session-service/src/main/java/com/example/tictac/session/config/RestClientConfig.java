package com.example.tictac.session.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({GameEngineProperties.class, SimulationProperties.class})
public class RestClientConfig {

	/** Runs before other {@link RestClientCustomizer}s so test mocks can wrap this factory. */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	RestClientCustomizer gameEngineRestClientCustomizer(GameEngineProperties properties) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
				.withConnectTimeout(properties.connectTimeout())
				.withReadTimeout(properties.readTimeout());

		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

		return builder -> builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory);
	}

	/**
	 * {@link RestClient} preconfigured to talk to the Game Engine Service.
	 */
	@Bean
	public RestClient gameEngineRestClient(RestClient.Builder restClientBuilder) {
		return restClientBuilder.build();
	}
}
