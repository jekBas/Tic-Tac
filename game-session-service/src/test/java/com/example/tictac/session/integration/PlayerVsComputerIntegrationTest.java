package com.example.tictac.session.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.session.dto.SessionResponse;
import com.example.tictac.session.integration.support.BaseSessionIntegrationTest;
import com.example.tictac.session.model.enums.SessionMode;
import com.example.tictac.session.model.enums.SessionStatus;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class PlayerVsComputerIntegrationTest extends BaseSessionIntegrationTest {

	private static final String GAME_ID = "engine-pvc-game-123";
	private static final Map<String, String> ENGINE_VARS = Map.of("gameId", GAME_ID);

	@Nested
	@DisplayName("POST /sessions/player-vs-computer")
	class CreatePvcSessionTests {

		@Test
		@DisplayName("Human as X -> session created, no computer move yet")
		void createsSessionAsX() {
			gameEngine.stubCreateGame(
					"create-session/engine-new-state-201/engine-create-game.json", ENGINE_VARS);

			ResponseEntity<SessionResponse> response = createPvcSession("X");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
			SessionResponse body = response.getBody();
			assertThat(body).isNotNull();
			assertThat(body.mode()).isEqualTo(SessionMode.PLAYER_VS_COMPUTER);
			assertThat(body.humanPlayer()).isNotNull();
			assertThat(body.humanPlayer().name()).isEqualTo("X");
			assertThat(body.status()).isEqualTo(SessionStatus.IN_PROGRESS);
			assertThat(body.moveHistory()).isEmpty();
			gameEngine.verifyMoveCalled(0);
		}

		@Test
		@DisplayName("Human as O -> session created with computer first move")
		void createsSessionAsO() {
			gameEngine.stubCreateGame(
					"create-session/engine-new-state-201/engine-create-game.json", ENGINE_VARS);
			gameEngine.stubMoveSequence(
					java.util.List.of("simulate-session/nine-move-draw/move-1-x-center.json"),
					ENGINE_VARS, 0L);

			ResponseEntity<SessionResponse> response = createPvcSession("O");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
			SessionResponse body = response.getBody();
			assertThat(body).isNotNull();
			assertThat(body.status()).isEqualTo(SessionStatus.IN_PROGRESS);
			assertThat(body.moveHistory()).hasSize(1);
			assertThat(body.moveHistory().get(0).player().name()).isEqualTo("X");
			gameEngine.verifyMoveCalled(1);
		}

		@Test
		@DisplayName("Missing humanPlayer returns 400")
		void missingHumanPlayerReturnsBadRequest() {
			ResponseEntity<String> response = restTemplate.exchange(
					"/sessions/player-vs-computer",
					HttpMethod.POST,
					jsonEntity("{}"),
					String.class);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		}
	}

	@Nested
	@DisplayName("POST /sessions/{id}/human-move")
	class HumanMoveTests {

		@Test
		@DisplayName("Valid human move triggers computer response")
		void humanMoveAndComputerResponse() {
			gameEngine.stubCreateGame(
					"create-session/engine-new-state-201/engine-create-game.json", ENGINE_VARS);
			SessionResponse created = createPvcSession("X").getBody();
			assertThat(created).isNotNull();

			gameEngine.stubMoveSequence(
					java.util.List.of(
							"simulate-session/nine-move-draw/move-1-x-center.json",
							"simulate-session/nine-move-draw/move-2-o-tl.json"),
					ENGINE_VARS, 0L);

			ResponseEntity<SessionResponse> response = submitHumanMove(
					created.sessionId(), 4);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			SessionResponse body = response.getBody();
			assertThat(body).isNotNull();
			assertThat(body.moveHistory()).hasSize(2);
			assertThat(body.moveHistory().get(0).player().name()).isEqualTo("X");
			assertThat(body.moveHistory().get(0).position()).isEqualTo(4);
			assertThat(body.moveHistory().get(1).player().name()).isEqualTo("O");
			assertThat(body.status()).isEqualTo(SessionStatus.IN_PROGRESS);
			gameEngine.verifyMoveCalled(2);
		}

		@Test
		@DisplayName("Human move that wins the game completes session without computer response")
		void humanWinsGame() {
			gameEngine.stubCreateGame(
					"create-session/engine-new-state-201/engine-create-game.json", ENGINE_VARS);
			SessionResponse created = createPvcSession("X").getBody();
			assertThat(created).isNotNull();

			String winningMoveJson = """
					{
					  "game-id": "${gameId}",
					  "board": ["X", "X", "X", null, null, null, null, null, null],
					  "status": "X_WON",
					  "winner": "X",
					  "next-player": null
					}
					""".replace("${gameId}", GAME_ID);
			gameEngine.stubMoveWithStatus(200, winningMoveJson);

			ResponseEntity<SessionResponse> response = submitHumanMove(
					created.sessionId(), 2);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			SessionResponse body = response.getBody();
			assertThat(body).isNotNull();
			assertThat(body.status()).isEqualTo(SessionStatus.COMPLETED);
			assertThat(body.currentGameState().status()).isEqualTo(GameStatus.X_WON);
			assertThat(body.moveHistory()).hasSize(1);
			gameEngine.verifyMoveCalled(1);
		}

		@Test
		@DisplayName("Move on completed session returns 409")
		void moveOnCompletedSessionReturnsConflict() {
			gameEngine.stubCreateGame(
					"create-session/engine-new-state-201/engine-create-game.json", ENGINE_VARS);
			SessionResponse created = createPvcSession("X").getBody();
			assertThat(created).isNotNull();

			String winningMoveJson = """
					{
					  "game-id": "${gameId}",
					  "board": ["X", "X", "X", null, null, null, null, null, null],
					  "status": "X_WON",
					  "winner": "X",
					  "next-player": null
					}
					""".replace("${gameId}", GAME_ID);
			gameEngine.stubMoveWithStatus(200, winningMoveJson);
			submitHumanMove(created.sessionId(), 2);

			ResponseEntity<String> retry = restTemplate.exchange(
					"/sessions/{id}/human-move",
					HttpMethod.POST,
					jsonEntity("{\"position\":5}"),
					String.class,
					created.sessionId());

			assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		}

		@Test
		@DisplayName("Move on non-PVC session returns 409")
		void moveOnSimulationSessionReturnsConflict() {
			gameEngine.stubCreateGame(
					"create-session/engine-new-state-201/engine-create-game.json", ENGINE_VARS);
			ResponseEntity<SessionResponse> simSession = createSession();
			assertThat(simSession.getBody()).isNotNull();

			ResponseEntity<String> response = restTemplate.exchange(
					"/sessions/{id}/human-move",
					HttpMethod.POST,
					jsonEntity("{\"position\":4}"),
					String.class,
					simSession.getBody().sessionId());

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		}

		@Test
		@DisplayName("Unknown session returns 404")
		void moveOnMissingSessionReturnsNotFound() {
			ResponseEntity<String> response = restTemplate.exchange(
					"/sessions/{id}/human-move",
					HttpMethod.POST,
					jsonEntity("{\"position\":4}"),
					String.class,
					"does-not-exist");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}

		@Test
		@DisplayName("Engine error on human move propagates as 502")
		void engineErrorPropagatesAsBadGateway() {
			gameEngine.stubCreateGame(
					"create-session/engine-new-state-201/engine-create-game.json", ENGINE_VARS);
			SessionResponse created = createPvcSession("X").getBody();
			assertThat(created).isNotNull();

			gameEngine.stubMoveWithStatus(500, "{\"message\":\"Engine crashed\"}");

			ResponseEntity<String> response = restTemplate.exchange(
					"/sessions/{id}/human-move",
					HttpMethod.POST,
					jsonEntity("{\"position\":4}"),
					String.class,
					created.sessionId());

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		}
	}

	private ResponseEntity<SessionResponse> createPvcSession(String player) {
		return restTemplate.exchange(
				"/sessions/player-vs-computer",
				HttpMethod.POST,
				jsonEntity("{\"human-player\":\"" + player + "\"}"),
				SessionResponse.class);
	}

	private ResponseEntity<SessionResponse> submitHumanMove(String sessionId, int position) {
		return restTemplate.exchange(
				"/sessions/{id}/human-move",
				HttpMethod.POST,
				jsonEntity("{\"position\":" + position + "}"),
				SessionResponse.class,
				sessionId);
	}

	private static HttpEntity<String> jsonEntity(String json) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(json, headers);
	}
}