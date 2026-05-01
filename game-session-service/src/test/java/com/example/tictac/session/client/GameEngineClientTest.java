package com.example.tictac.session.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.dto.MoveRequest;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import com.example.tictac.session.GameSessionServiceApplication;
import com.example.tictac.session.config.GameEngineProperties;
import com.example.tictac.session.exception.GameEngineCommunicationException;
import com.example.tictac.session.exception.GameEngineTransientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;

@SpringBootTest(
		classes = GameSessionServiceApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMockRestServiceServer
@ExtendWith(OutputCaptureExtension.class)
class GameEngineClientTest {

	private static final String GAME_ID = "game-123";

	@Autowired
	private GameEngineClient gameEngineClient;

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private GameEngineProperties gameEngineProperties;

	@BeforeEach
	void resetMockServer() {
		server.reset();
	}

	private String enginePath(String path) {
		return gameEngineProperties.baseUrl() + path;
	}

	@Test
	void createGameReturnsGameState() throws JsonProcessingException {
		GameStateDto expected = new GameStateDto(
				GAME_ID, Collections.nCopies(9, (String) null), GameStatus.NEW, null, null);

		server.expect(requestTo(enginePath("/games")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

		GameStateDto result = gameEngineClient.createGame();

		assertThat(result.gameId()).isEqualTo(GAME_ID);
		assertThat(result.status()).isEqualTo(GameStatus.NEW);
		assertThat(result.board()).hasSize(9);
		server.verify();
	}

	@Test
	void applyMoveReturnsUpdatedGameState() throws JsonProcessingException {
		GameStateDto expected = new GameStateDto(
				GAME_ID,
				Arrays.asList(null, null, null, null, "X", null, null, null, null),
				GameStatus.IN_PROGRESS, null, Player.O);
		MoveRequest moveRequest = new MoveRequest(Player.X, 4);

		server.expect(requestTo(enginePath("/games/" + GAME_ID + "/move")))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json(objectMapper.writeValueAsString(moveRequest)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

		GameStateDto result = gameEngineClient.applyMove(GAME_ID, moveRequest);

		assertThat(result.gameId()).isEqualTo(GAME_ID);
		assertThat(result.status()).isEqualTo(GameStatus.IN_PROGRESS);
		assertThat(result.nextPlayer()).isEqualTo(Player.O);
		server.verify();
	}

	@Test
	void getGameReturnsGameState() throws JsonProcessingException {
		GameStateDto expected = new GameStateDto(
				GAME_ID, Collections.nCopies(9, (String) null), GameStatus.IN_PROGRESS, null, Player.X);

		server.expect(requestTo(enginePath("/games/" + GAME_ID)))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

		GameStateDto result = gameEngineClient.getGame(GAME_ID);

		assertThat(result.gameId()).isEqualTo(GAME_ID);
		assertThat(result.status()).isEqualTo(GameStatus.IN_PROGRESS);
		assertThat(result.nextPlayer()).isEqualTo(Player.X);
		server.verify();
	}

	@Test
	void createGameThrowsOnServerError() {
		server.expect(times(3), requestTo(enginePath("/games")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withServerError()
						.body("Internal Server Error"));

		assertThatThrownBy(() -> gameEngineClient.createGame())
				.isInstanceOf(GameEngineTransientException.class)
				.hasMessageContaining("Game Engine")
				.hasMessageContaining("500");

		server.verify();
	}

	@Test
	void applyMoveThrowsOnBadRequest() {
		server.expect(requestTo(enginePath("/games/" + GAME_ID + "/move")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST)
						.body("{\"message\":\"Cell 4 is already occupied\"}"));

		assertThatThrownBy(() -> gameEngineClient.applyMove(GAME_ID, new MoveRequest(Player.X, 4)))
				.isInstanceOf(GameEngineCommunicationException.class)
				.hasMessageContaining("Game Engine")
				.hasMessageContaining("400");

		server.verify();
	}

	@Test
	void getGameThrowsOnNotFound() {
		server.expect(requestTo(enginePath("/games/" + GAME_ID)))
				.andExpect(method(HttpMethod.GET))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND)
						.body("{\"message\":\"Game not found\"}"));

		assertThatThrownBy(() -> gameEngineClient.getGame(GAME_ID))
				.isInstanceOf(GameEngineCommunicationException.class)
				.hasMessageContaining("Game Engine")
				.hasMessageContaining("404");

		server.verify();
	}

	@Test
	void createGameThrowsOnConnectionFailure() {
		server.expect(times(3), requestTo(enginePath("/games")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(request -> {
					throw new IOException("Connection refused");
				});

		assertThatThrownBy(() -> gameEngineClient.createGame())
				.isInstanceOf(GameEngineTransientException.class)
				.hasMessageContaining("Game Engine")
				.hasMessageContaining("unreachable");

		server.verify();
	}

	@Test
	void createGameRecoversAfterTransientFailure() throws JsonProcessingException {
		GameStateDto expected = new GameStateDto(
				GAME_ID, Collections.nCopies(9, (String) null), GameStatus.NEW, null, null);

		server.expect(ExpectedCount.once(), requestTo(enginePath("/games")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withServerError().body("temporary"));

		server.expect(ExpectedCount.once(), requestTo(enginePath("/games")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

		GameStateDto result = gameEngineClient.createGame();

		assertThat(result.gameId()).isEqualTo(GAME_ID);
		assertThat(result.status()).isEqualTo(GameStatus.NEW);
		server.verify();
	}

	@Test
	void applyMoveRecoversAfterTransientFailure() throws JsonProcessingException {
		GameStateDto expected = new GameStateDto(
				GAME_ID,
				Arrays.asList(null, null, null, null, "X", null, null, null, null),
				GameStatus.IN_PROGRESS, null, Player.O);
		MoveRequest moveRequest = new MoveRequest(Player.X, 4);

		server.expect(ExpectedCount.once(), requestTo(enginePath("/games/" + GAME_ID + "/move")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withServerError().body("temporary"));

		server.expect(ExpectedCount.once(), requestTo(enginePath("/games/" + GAME_ID + "/move")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

		GameStateDto result = gameEngineClient.applyMove(GAME_ID, moveRequest);

		assertThat(result.gameId()).isEqualTo(GAME_ID);
		assertThat(result.status()).isEqualTo(GameStatus.IN_PROGRESS);
		server.verify();
	}

	@Test
	void retryLogsWarningOnEachFailedAttempt(CapturedOutput output) {
		server.expect(times(3), requestTo(enginePath("/games")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withServerError()
						.body("Internal Server Error"));

		assertThatThrownBy(() -> gameEngineClient.createGame())
				.isInstanceOf(GameEngineTransientException.class);

		assertThat(output).contains("Game Engine call failed (attempt");
		server.verify();
	}

	@Test
	void retryLogsErrorWhenAllAttemptsExhausted(CapturedOutput output) {
		server.expect(times(3), requestTo(enginePath("/games")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withServerError()
						.body("Internal Server Error"));

		assertThatThrownBy(() -> gameEngineClient.createGame())
				.isInstanceOf(GameEngineTransientException.class);

		assertThat(output).contains("All retry attempts exhausted for Game Engine");
		server.verify();
	}

	@Test
	void nonRetryableExceptionDoesNotRetry() {
		server.expect(ExpectedCount.once(), requestTo(enginePath("/games/" + GAME_ID)))
				.andExpect(method(HttpMethod.GET))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND)
						.body("{\"message\":\"Game not found\"}"));

		assertThatThrownBy(() -> gameEngineClient.getGame(GAME_ID))
				.isInstanceOf(GameEngineCommunicationException.class)
				.isNotInstanceOf(GameEngineTransientException.class);

		server.verify();
	}
}
