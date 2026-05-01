package com.example.tictac.engine.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.tictac.engine.controller.arguments.ApplyMoveArguments;
import com.example.tictac.common.dto.MoveRequest;
import com.example.tictac.engine.exception.GameNotFoundException;
import com.example.tictac.engine.exception.InvalidMoveException;
import com.example.tictac.common.GameConstants;
import com.example.tictac.engine.model.Game;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import com.example.tictac.engine.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(GameController.class)
class GameControllerTest {

	private static final String GAME_ID = "game-123";
	private static final String GAME_PATH = "/games/" + GAME_ID;
	private static final String MOVE_PATH = GAME_PATH + "/move";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private GameService gameService;

	@Test
	void createGameReturnsCreatedWithFreshGame() throws Exception {
		Game game = new Game(GAME_ID);
		given(gameService.createGame()).willReturn(game);

		mockMvc.perform(post("/games"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$['game-id']", is(GAME_ID)))
				.andExpect(jsonPath("$.status", is(GameStatus.NEW.name())))
				.andExpect(jsonPath("$.winner").value(nullValue()))
				.andExpect(jsonPath("$['next-player']", is(Player.X.name())))
				.andExpect(jsonPath("$.board", hasSize(GameConstants.BOARD_SIZE)));
	}

	@Test
	void getGameReturnsGameWhenFound() throws Exception {
		Game game = new Game(GAME_ID);
		game.setStatus(GameStatus.IN_PROGRESS);
		given(gameService.getGame(GAME_ID)).willReturn(game);

		mockMvc.perform(get("/games/{gameId}", GAME_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$['game-id']", is(GAME_ID)))
				.andExpect(jsonPath("$.status", is(GameStatus.IN_PROGRESS.name())))
				.andExpect(jsonPath("$['next-player']", is(Player.X.name())));
	}

	@Test
	void getGameReturnsNotFoundWhenServiceThrows() throws Exception {
		given(gameService.getGame(GAME_ID)).willThrow(new GameNotFoundException(GAME_ID));

		ResultActions result = mockMvc.perform(get("/games/{gameId}", GAME_ID))
				.andExpect(status().isNotFound());

		expectFullErrorResponse(result, HttpStatus.NOT_FOUND, GAME_PATH, containsString(GAME_ID));
	}

	@Test
	void applyMoveReturnsOkOnSuccess() throws Exception {
		Game game = new Game(GAME_ID);
		game.setStatus(GameStatus.IN_PROGRESS);
		game.setNextPlayer(Player.O);
		game.getBoard()[4] = Player.X;
		given(gameService.applyMove(eq(GAME_ID), any(MoveRequest.class))).willReturn(game);

		MoveRequest request = new MoveRequest(Player.X, 4);

		mockMvc.perform(post("/games/{gameId}/move", GAME_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$['game-id']", is(GAME_ID)))
				.andExpect(jsonPath("$.status", is(GameStatus.IN_PROGRESS.name())))
				.andExpect(jsonPath("$['next-player']", is(Player.O.name())))
				.andExpect(jsonPath("$.board[4]", is(Player.X.name())));
	}

	@Test
	void applyMoveReturnsBadRequestForInvalidMoveException() throws Exception {
		given(gameService.applyMove(eq(GAME_ID), any(MoveRequest.class)))
				.willThrow(new InvalidMoveException("Cell 4 is already occupied"));

		MoveRequest request = new MoveRequest(Player.X, 4);

		ResultActions result = mockMvc.perform(post("/games/{gameId}/move", GAME_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		expectFullErrorResponse(result, HttpStatus.BAD_REQUEST, MOVE_PATH,
				containsString("already occupied"));
	}

	@Test
	void applyMoveReturnsBadRequestWhenPlayerIsNull() throws Exception {
		String payload = """
				{ "player": null, "position": 4 }
				""";

		ResultActions result = mockMvc.perform(post("/games/{gameId}/move", GAME_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest());

		expectFullErrorResponse(result, HttpStatus.BAD_REQUEST, MOVE_PATH, containsString("player"));

		verifyNoInteractions(gameService);
	}

	@Test
	void applyMoveReturnsBadRequestWhenBodyIsMalformed() throws Exception {
		ResultActions result = mockMvc.perform(post("/games/{gameId}/move", GAME_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not-json"))
				.andExpect(status().isBadRequest());

		expectFullErrorResponse(result, HttpStatus.BAD_REQUEST, MOVE_PATH, containsString("JSON"));

		verifyNoInteractions(gameService);
	}

	@ParameterizedTest(name = "position {0} is rejected with 400")
	@ArgumentsSource(ApplyMoveArguments.InvalidPositions.class)
	void applyMoveRejectsInvalidPositions(Integer position) throws Exception {
		String payload = String.format("{ \"player\": \"X\", \"position\": %s }",
				position == null ? "null" : position);

		ResultActions result = mockMvc.perform(post("/games/{gameId}/move", GAME_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest());

		expectFullErrorResponse(result, HttpStatus.BAD_REQUEST, MOVE_PATH, containsString("position"));

		verifyNoInteractions(gameService);
	}

	@ParameterizedTest(name = "position {0} is accepted")
	@ArgumentsSource(ApplyMoveArguments.ValidPositions.class)
	void applyMoveAcceptsValidPositions(int position) throws Exception {
		Game game = new Game(GAME_ID);
		game.setStatus(GameStatus.IN_PROGRESS);
		game.setNextPlayer(Player.O);
		game.getBoard()[position] = Player.X;
		given(gameService.applyMove(eq(GAME_ID), any(MoveRequest.class))).willReturn(game);

		MoveRequest request = new MoveRequest(Player.X, position);

		mockMvc.perform(post("/games/{gameId}/move", GAME_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
	}

	private static void expectFullErrorResponse(ResultActions result,
																							HttpStatus httpStatus,
																							String path,
																							Matcher<?> messageMatcher) throws Exception {
		result
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status", is(httpStatus.value())))
				.andExpect(jsonPath("$.error", is(httpStatus.getReasonPhrase())))
				.andExpect(jsonPath("$.message", messageMatcher))
				.andExpect(jsonPath("$.path", is(path)));
	}
}
