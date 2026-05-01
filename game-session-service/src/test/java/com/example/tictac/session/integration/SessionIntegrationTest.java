package com.example.tictac.session.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.session.dto.SessionResponse;
import com.example.tictac.session.integration.argument.TestDataSessionArgumentsProvider;
import com.example.tictac.session.integration.support.BaseSessionIntegrationTest;
import com.example.tictac.session.integration.support.JsonFixtures;
import com.example.tictac.session.model.enums.SessionStatus;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end integration tests for {@code game-session-service} that exercise
 * the real Spring web stack and a WireMock-backed Game Engine.
 */
class SessionIntegrationTest extends BaseSessionIntegrationTest {

	private static final String GAME_ID = "engine-game-abc-123";
	private static final Map<String, String> ENGINE_VARS = Map.of("gameId", GAME_ID);

	private static final String[] IGNORED_FIELDS = {
			"sessionId",
			"createdAt",
			"updatedAt",
			"moveHistory.createdAt"
	};

	@Nested
	@DisplayName("POST /sessions")
	class CreateSessionTests {

		@ParameterizedTest(name = "[{index}] {0}")
		@ArgumentsSource(TestDataSessionArgumentsProvider.CreateSession.class)
		void createsSessionAndReturnsBody(String testName,
																			String gameEngineCreateResponsePath,
																			String expectedSessionResponsePath,
																			HttpStatus expectedHttpStatus) {
			// Arrange
			gameEngine.stubCreateGame(gameEngineCreateResponsePath, ENGINE_VARS);

			// Act
			ResponseEntity<SessionResponse> response = createSession();

			// Assert
			SessionResponse expected = JsonFixtures.readObjectFromFilePath(
					expectedSessionResponsePath, SessionResponse.class, ENGINE_VARS);

			assertThat(response.getStatusCode()).isEqualTo(expectedHttpStatus);
			assertThat(response.getBody())
					.isNotNull()
					.usingRecursiveComparison()
					.ignoringFields(IGNORED_FIELDS)
					.isEqualTo(expected);

			SessionResponse actual = response.getBody();
			assertThat(actual.sessionId()).isNotBlank();
			assertThat(actual.gameId()).isEqualTo(GAME_ID);
			assertThat(actual.status()).isEqualTo(SessionStatus.CREATED);
			assertThat(actual.createdAt()).isNotNull();
			assertThat(actual.moveHistory()).isEmpty();

			gameEngine.verifyCreateGameCalled(1);
			gameEngine.verifyMoveCalled(0);
		}

		@Test
		@DisplayName("Engine error on POST /games surfaces as 502 BAD_GATEWAY")
		void createSessionReturnsBadGatewayWhenEngineFails() {
			gameEngine.stubCreateGameWithStatus(500, "Engine crashed");

			ResponseEntity<String> response = restTemplate.postForEntity(
					"/sessions", null, String.class);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
			assertThat(sessionRepository.findById("any")).isEmpty();
			gameEngine.verifyCreateGameCalled(1);
		}
	}

	@Nested
	@DisplayName("GET /sessions/{id}")
	class GetSessionTests {

		@ParameterizedTest(name = "[{index}] {0}")
		@ArgumentsSource(TestDataSessionArgumentsProvider.GetSession.class)
		void returnsExistingSession(String testName,
																String gameEngineCreateResponsePath,
																String expectedSessionResponsePath,
																HttpStatus expectedHttpStatus) {
			gameEngine.stubCreateGame(gameEngineCreateResponsePath, ENGINE_VARS);
			SessionResponse created = createSession().getBody();
			assertThat(created).isNotNull();

			ResponseEntity<SessionResponse> response = getSession(created.sessionId());

			SessionResponse expected = JsonFixtures.readObjectFromFilePath(
					expectedSessionResponsePath, SessionResponse.class, ENGINE_VARS);
			assertThat(response.getStatusCode()).isEqualTo(expectedHttpStatus);
			assertThat(response.getBody())
					.isNotNull()
					.usingRecursiveComparison()
					.ignoringFields(IGNORED_FIELDS)
					.isEqualTo(expected);
			assertThat(response.getBody().sessionId()).isEqualTo(created.sessionId());
		}

		@Test
		@DisplayName("Unknown sessionId returns 404 NOT_FOUND")
		void returnsNotFoundForMissingSession() {
			ResponseEntity<String> response = restTemplate.getForEntity(
					"/sessions/{id}", String.class, "does-not-exist");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(response.getBody()).contains("does-not-exist");
		}
	}

	@Nested
	@DisplayName("POST /sessions/{id}/simulate")
	class SimulateSessionTests {

		@ParameterizedTest(name = "[{index}] {0}")
		@ArgumentsSource(TestDataSessionArgumentsProvider.SimulateSession.class)
		void runsFullSimulationToTerminalState(String testName,
																					 String gameEngineCreateResponsePath,
																					 List<String> gameEngineMoveResponsePaths,
																					 String expectedSessionResponsePath,
																					 HttpStatus expectedHttpStatus) {
			// Arrange — engine accepts game creation and serves the move sequence in order
			gameEngine.stubCreateGame(gameEngineCreateResponsePath, ENGINE_VARS);
			gameEngine.stubMoveSequence(gameEngineMoveResponsePaths, ENGINE_VARS, 0L);

			SessionResponse created = createSession().getBody();
			assertThat(created).isNotNull();

			// Act
			ResponseEntity<SessionResponse> response = simulate(created.sessionId());

			// Assert — HTTP shape
			assertThat(response.getStatusCode()).isEqualTo(expectedHttpStatus);
			SessionResponse actual = response.getBody();
			assertThat(actual).isNotNull();

			// Recursive comparison against the canonical fixture
			SessionResponse expected = JsonFixtures.readObjectFromFilePath(
					expectedSessionResponsePath, SessionResponse.class, ENGINE_VARS);
			assertThat(actual)
					.usingRecursiveComparison()
					.ignoringFields(IGNORED_FIELDS)
					.isEqualTo(expected);

			// Explicit assertions on generated/non-deterministic fields
			assertThat(actual.sessionId()).isEqualTo(created.sessionId()).isNotBlank();
			assertThat(actual.gameId()).isEqualTo(GAME_ID);
			assertThat(actual.status()).isEqualTo(SessionStatus.COMPLETED);
			assertThat(actual.failureReason()).isNull();

			// Move history length and order are correct
			assertThat(actual.moveHistory()).hasSize(gameEngineMoveResponsePaths.size());
			assertThat(actual.moveHistory())
					.extracting("moveNumber")
					.containsExactlyElementsOf(numbersFromOneTo(actual.moveHistory().size()));
			assertThat(actual.moveHistory().get(actual.moveHistory().size() - 1).resultingStatus())
					.isIn(GameStatus.X_WON, GameStatus.O_WON, GameStatus.DRAW);

			// And the engine was called the expected number of times
			gameEngine.verifyCreateGameCalled(1);
			gameEngine.verifyMoveCalled(gameEngineMoveResponsePaths.size());
		}

		@Test
		@DisplayName("Simulating an unknown session yields 404 NOT_FOUND")
		void simulateUnknownSessionReturnsNotFound() {
			ResponseEntity<String> response = simulateRaw("missing-session");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(response.getBody()).contains("missing-session");
			gameEngine.verifyMoveCalled(0);
		}
	}

	@Nested
	@DisplayName("Error handling")
	class ErrorHandlingTests {

		@ParameterizedTest(name = "[{index}] {0}")
		@ArgumentsSource(TestDataSessionArgumentsProvider.ErrorScenarios.class)
		void engineFailuresPropagateAsBadGateway(
				String testName,
				int engineStatus,
				String engineBody,
				HttpStatus expectedHttpStatus) {
			// Arrange
			gameEngine.stubCreateGame(
					"create-session/engine-new-state-201/engine-create-game.json", ENGINE_VARS);
			gameEngine.stubMoveWithStatus(engineStatus, engineBody);
			SessionResponse created = createSession().getBody();
			assertThat(created).isNotNull();

			// Act
			ResponseEntity<String> response = simulateRaw(created.sessionId());

			// Assert HTTP contract
			assertThat(response.getStatusCode()).isEqualTo(expectedHttpStatus);
			assertThat(response.getBody()).contains("Game Engine");

			// And the session has transitioned to FAILED
			SessionResponse afterFailure = getSession(created.sessionId()).getBody();
			assertThat(afterFailure).isNotNull();
			assertThat(afterFailure.status()).isEqualTo(SessionStatus.FAILED);
			assertThat(afterFailure.failureReason()).isNotBlank();

			// A FAILED session cannot be simulated again -> 409 CONFLICT
			ResponseEntity<String> retry = simulateRaw(created.sessionId());
			assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		}

		@Test
		@DisplayName("Engine connection reset -> 502 BAD_GATEWAY and session FAILED")
		void engineConnectionResetReturnsBadGateway() {
			gameEngine.stubCreateGameConnectionFailure();

			ResponseEntity<String> response = restTemplate.postForEntity(
					"/sessions", null, String.class);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
			assertThat(response.getBody()).contains("Game Engine");
		}
	}

	@Nested
	@DisplayName("Concurrency")
	class ConcurrencyTests {

		@Test
		@DisplayName("Two parallel simulates: one wins, the other gets 409 or the completed body")
		void parallelSimulatesAreSerialised() throws InterruptedException, ExecutionException, TimeoutException {
			// Arrange — engine is artificially slow so the first simulate stays in flight
			// long enough for the second to overlap. The first move is the only one
			// served; the engine reports a terminal state immediately afterwards so
			// the simulation completes after a single move.
			gameEngine.stubCreateGame(
					"simulate-session/nine-move-draw/engine-create-game.json", ENGINE_VARS);
			gameEngine.stubMoveSlow("simulate-session/nine-move-draw/move-9-x-draw.json", 750L);

			SessionResponse created = createSession().getBody();
			assertThat(created).isNotNull();
			String sessionId = created.sessionId();

			ExecutorService pool = Executors.newFixedThreadPool(2);
			try {
				// Act — two concurrent simulations on the same session
				CompletableFuture<ResponseEntity<String>> first = CompletableFuture.supplyAsync(
						() -> simulateRaw(sessionId), pool);
				// Tiny stagger so 'first' definitely owns the lock before 'second' arrives.
				Thread.sleep(50);
				CompletableFuture<ResponseEntity<String>> second = CompletableFuture.supplyAsync(
						() -> simulateRaw(sessionId), pool);

				ResponseEntity<String> firstResponse = first.get(10, TimeUnit.SECONDS);
				ResponseEntity<String> secondResponse = second.get(10, TimeUnit.SECONDS);

				// Assert — exactly one of the two is OK; the other is 409 (lock held) or
				// 200 with the completed body (if the second one arrived after the first
				// already finished and saw COMPLETED).
				List<HttpStatus> statuses = List.of(
						(HttpStatus) firstResponse.getStatusCode(),
						(HttpStatus) secondResponse.getStatusCode());
				assertThat(statuses).containsAnyOf(HttpStatus.OK);
				assertThat(statuses)
						.allMatch(s -> s == HttpStatus.OK || s == HttpStatus.CONFLICT,
								"Each response should be either 200 OK or 409 CONFLICT");
				assertThat(statuses.stream().filter(HttpStatus.OK::equals).count())
						.as("at least one simulate must succeed")
						.isGreaterThanOrEqualTo(1);

				// Final state is consistent: COMPLETED with exactly one recorded move.
				SessionResponse finalState = getSession(sessionId).getBody();
				assertThat(finalState).isNotNull();
				assertThat(finalState.status()).isEqualTo(SessionStatus.COMPLETED);
				assertThat(finalState.moveHistory()).hasSize(1);

				// And the engine only ever saw a single move — the second simulate
				// either bounced on the lock or saw COMPLETED and returned without
				// touching the engine.
				gameEngine.verifyMoveCalled(1);
			} finally {
				pool.shutdownNow();
				assertThatCode(() -> pool.awaitTermination(5, TimeUnit.SECONDS)).doesNotThrowAnyException();
			}
		}
	}
}
