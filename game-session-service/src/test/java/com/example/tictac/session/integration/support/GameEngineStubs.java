package com.example.tictac.session.integration.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Fluent helpers around a {@link WireMockServer} that mock the Game Engine
 * Service's HTTP contract:
 * <ul>
 *   <li>{@code POST /games}</li>
 *   <li>{@code POST /games/{gameId}/move}</li>
 *   <li>{@code GET  /games/{gameId}}</li>
 * </ul>
 *
 * <p>Per-call state is stored on the underlying server; create a fresh
 * {@code GameEngineStubs} for each test (the base class does this in
 * {@code @BeforeEach}).
 */
public final class GameEngineStubs {

	private static final String CREATE_GAME_PATH = "/games";
	private static final String MOVE_PATH_REGEX = "/games/[^/]+/move";

	private final WireMockServer server;

	public GameEngineStubs(WireMockServer server) {
		this.server = server;
	}

	/** Stubs {@code POST /games} to return a 201 with the body loaded from {@code fixturePath}. */
	public GameEngineStubs stubCreateGame(String fixturePath, Map<String, String> variables) {
		String body = JsonFixtures.getJsonFromFilePath(fixturePath, variables);
		server.stubFor(post(urlEqualTo(CREATE_GAME_PATH))
				.willReturn(aResponse()
						.withStatus(HttpStatus.CREATED.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(body)));
		return this;
	}

	public GameEngineStubs stubCreateGameWithStatus(int status, String body) {
		server.stubFor(post(urlEqualTo(CREATE_GAME_PATH))
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(body == null ? "" : body)));
		return this;
	}

	/** Simulates a TCP-level connection failure when the engine is contacted. */
	public GameEngineStubs stubCreateGameConnectionFailure() {
		server.stubFor(post(urlEqualTo(CREATE_GAME_PATH))
				.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
		return this;
	}

	/**
	 * Stubs {@code POST /games/{gameId}/move} so that successive invocations
	 * return the JSON bodies loaded from {@code fixturePaths} in order.
	 */
	public GameEngineStubs stubMoveSequence(List<String> fixturePaths,
																					Map<String, String> variables,
																					long delayMillis) {
		if (fixturePaths == null || fixturePaths.isEmpty()) {
			throw new IllegalArgumentException("At least one move fixture is required");
		}

		String scenario = "moves-" + System.nanoTime();
		String previous = Scenario.STARTED;
		for (int i = 0; i < fixturePaths.size(); i++) {
			String body = JsonFixtures.getJsonFromFilePath(fixturePaths.get(i), variables);
			boolean last = i == fixturePaths.size() - 1;
			String nextState = last ? previous : "after-move-" + (i + 1);

			server.stubFor(post(urlMatching(MOVE_PATH_REGEX))
					.inScenario(scenario)
					.whenScenarioStateIs(previous)
					.willSetStateTo(nextState)
					.willReturn(buildMoveResponse(body, delayMillis)));

			previous = nextState;
		}
		return this;
	}

	public GameEngineStubs stubMoveWithStatus(int status, String body) {
		server.stubFor(post(urlMatching(MOVE_PATH_REGEX))
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(body == null ? "" : body)));
		return this;
	}

	/** Forces the move endpoint to delay every response by {@code delayMillis}. */
	public GameEngineStubs stubMoveSlow(String fixturePath, long delayMillis) {
		String body = JsonFixtures.getJsonFromFilePath(fixturePath);
		server.stubFor(post(urlMatching(MOVE_PATH_REGEX))
				.willReturn(buildMoveResponse(body, delayMillis)));
		return this;
	}

	private static ResponseDefinitionBuilder buildMoveResponse(String body, long delayMillis) {
		var builder = aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.withBody(body);
		if (delayMillis > 0) {
			builder = builder.withFixedDelay((int) delayMillis);
		}
		return builder;
	}

	// Verification helpers
	public void verifyCreateGameCalled(int times) {
		server.verify(exactly(times), postRequestedFor(urlEqualTo(CREATE_GAME_PATH)));
	}

	public void verifyMoveCalled(int times) {
		server.verify(exactly(times), postRequestedFor(urlPathMatching(MOVE_PATH_REGEX)));
	}
}
