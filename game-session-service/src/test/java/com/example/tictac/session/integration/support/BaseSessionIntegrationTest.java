package com.example.tictac.session.integration.support;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.example.tictac.session.dto.SessionResponse;
import com.example.tictac.session.repository.SessionRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for full-stack integration tests of the {@code game-session-service}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public abstract class BaseSessionIntegrationTest {

	protected static final WireMockServer GAME_ENGINE_MOCK =
			new WireMockServer(options().dynamicPort());

	@LocalServerPort
	protected int serverPort;

	@Autowired
	protected TestRestTemplate restTemplate;
	@Autowired
	protected SessionRepository sessionRepository;

	protected GameEngineStubs gameEngine;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		if (!GAME_ENGINE_MOCK.isRunning()) {
			GAME_ENGINE_MOCK.start();
		}
		registry.add("game-engine.base-url", () -> "http://localhost:" + GAME_ENGINE_MOCK.port());
		registry.add("session.simulation.move-delay", () -> "0ms");
	}

	@AfterAll
	static void stopWireMock() {
		if (GAME_ENGINE_MOCK.isRunning()) {
			GAME_ENGINE_MOCK.stop();
		}
	}

	@BeforeEach
	void initialiseStubs() {
		GAME_ENGINE_MOCK.resetAll();
		gameEngine = new GameEngineStubs(GAME_ENGINE_MOCK);
	}

	@AfterEach
	void clearSessionState() {
		sessionRepository.clear();
	}

	protected ResponseEntity<SessionResponse> createSession() {
		return restTemplate.postForEntity("/sessions", null, SessionResponse.class);
	}

	protected ResponseEntity<SessionResponse> getSession(String sessionId) {
		return restTemplate.getForEntity("/sessions/{id}", SessionResponse.class, sessionId);
	}

	protected ResponseEntity<SessionResponse> simulate(String sessionId) {
		return restTemplate.exchange(
				"/sessions/{id}/simulate",
				HttpMethod.POST,
				HttpEntity.EMPTY,
				SessionResponse.class,
				sessionId);
	}

	protected ResponseEntity<String> simulateRaw(String sessionId) {
		return restTemplate.exchange(
				"/sessions/{id}/simulate",
				HttpMethod.POST,
				HttpEntity.EMPTY,
				String.class,
				sessionId);
	}

	protected static List<Integer> numbersFromOneTo(int n) {
		return IntStream.rangeClosed(1, n).boxed().toList();
	}
}
