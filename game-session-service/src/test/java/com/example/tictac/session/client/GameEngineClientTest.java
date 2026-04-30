package com.example.tictac.session.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.dto.MoveRequest;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import com.example.tictac.session.exception.GameEngineCommunicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

class GameEngineClientTest {

	private static final String GAME_ID = "game-123";

	private GameEngineClient gameEngineClient;
	private MockRestServiceServer server;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		gameEngineClient = new GameEngineClient(builder.build());
	}

	@Test
	void createGameReturnsGameState() throws JsonProcessingException {
		GameStateDto expected = new GameStateDto(
				GAME_ID, Collections.nCopies(9, (String) null), GameStatus.NEW, null, null);

		server.expect(requestTo("/games"))
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

		server.expect(requestTo("/games/" + GAME_ID + "/move"))
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

		server.expect(requestTo("/games/" + GAME_ID))
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
		server.expect(requestTo("/games"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withServerError()
						.body("Internal Server Error"));

		assertThatThrownBy(() -> gameEngineClient.createGame())
				.isInstanceOf(GameEngineCommunicationException.class)
				.hasMessageContaining("Game Engine")
				.hasMessageContaining("500");

		server.verify();
	}

	@Test
	void applyMoveThrowsOnBadRequest() throws JsonProcessingException {
		MoveRequest moveRequest = new MoveRequest(Player.X, 4);

		server.expect(requestTo("/games/" + GAME_ID + "/move"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST)
						.body("{\"message\":\"Cell 4 is already occupied\"}"));

		assertThatThrownBy(() -> gameEngineClient.applyMove(GAME_ID, moveRequest))
				.isInstanceOf(GameEngineCommunicationException.class)
				.hasMessageContaining("Game Engine")
				.hasMessageContaining("400");

		server.verify();
	}

	@Test
	void getGameThrowsOnNotFound() {
		server.expect(requestTo("/games/" + GAME_ID))
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
		server.expect(requestTo("/games"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(request -> {
					throw new IOException("Connection refused");
				});

		assertThatThrownBy(() -> gameEngineClient.createGame())
				.isInstanceOf(GameEngineCommunicationException.class)
				.hasMessageContaining("Game Engine")
				.hasMessageContaining("unreachable");

		server.verify();
	}
}
