package com.example.tictac.session.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import com.example.tictac.session.exception.GameEngineCommunicationException;
import com.example.tictac.session.exception.InvalidSessionStateException;
import com.example.tictac.session.exception.SessionNotFoundException;
import com.example.tictac.session.exception.SimulationAlreadyRunningException;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.model.MoveRecord;
import com.example.tictac.session.model.enums.SessionStatus;
import com.example.tictac.session.service.SessionService;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

	private static final String SESSION_ID = "session-123";
	private static final String GAME_ID = "game-456";
	private static final String SESSION_PATH = "/sessions/" + SESSION_ID;
	private static final String SIMULATE_PATH = SESSION_PATH + "/simulate";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SessionService sessionService;

	@Test
	void createSessionReturnsCreatedWithNewSession() throws Exception {
		GameSession session = createTestSession();
		given(sessionService.createSession()).willReturn(session);

		mockMvc.perform(post("/sessions"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$['session-id']", is(SESSION_ID)))
				.andExpect(jsonPath("$['game-id']", is(GAME_ID)))
				.andExpect(jsonPath("$.status", is(SessionStatus.CREATED.name())))
				.andExpect(jsonPath("$['move-history']", empty()));
	}

	@Test
	void getSessionReturnsOkWhenFound() throws Exception {
		GameSession session = createTestSession();
		given(sessionService.getSession(SESSION_ID)).willReturn(session);

		mockMvc.perform(get("/sessions/{sessionId}", SESSION_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$['session-id']", is(SESSION_ID)))
				.andExpect(jsonPath("$['game-id']", is(GAME_ID)))
				.andExpect(jsonPath("$.status", is(SessionStatus.CREATED.name())));
	}

	@Test
	void getSessionReturnsNotFoundWhenServiceThrows() throws Exception {
		given(sessionService.getSession(SESSION_ID))
				.willThrow(new SessionNotFoundException(SESSION_ID));

		ResultActions result = mockMvc.perform(get("/sessions/{sessionId}", SESSION_ID))
				.andExpect(status().isNotFound());

		expectFullErrorResponse(result, HttpStatus.NOT_FOUND, SESSION_PATH, containsString(SESSION_ID));
	}

	@Test
	void simulateReturnsOkWithCompletedSession() throws Exception {
		GameSession session = createCompletedSession();
		given(sessionService.simulate(SESSION_ID)).willReturn(session);

		mockMvc.perform(post("/sessions/{sessionId}/simulate", SESSION_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$['session-id']", is(SESSION_ID)))
				.andExpect(jsonPath("$.status", is(SessionStatus.COMPLETED.name())))
				.andExpect(jsonPath("$['move-history'][0]['move-number']", is(1)))
				.andExpect(jsonPath("$['move-history'][0].player", is(Player.X.name())))
				.andExpect(jsonPath("$['move-history'][0].position", is(4)));
	}

	@Test
	void simulateReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
		given(sessionService.simulate(SESSION_ID))
				.willThrow(new SessionNotFoundException(SESSION_ID));

		ResultActions result = mockMvc.perform(post("/sessions/{sessionId}/simulate", SESSION_ID))
				.andExpect(status().isNotFound());

		expectFullErrorResponse(result, HttpStatus.NOT_FOUND, SIMULATE_PATH, containsString(SESSION_ID));
	}

	@Test
	void simulateReturnsConflictWhenAlreadyRunning() throws Exception {
		given(sessionService.simulate(SESSION_ID))
				.willThrow(new SimulationAlreadyRunningException(SESSION_ID));

		ResultActions result = mockMvc.perform(post("/sessions/{sessionId}/simulate", SESSION_ID))
				.andExpect(status().isConflict());

		expectFullErrorResponse(result, HttpStatus.CONFLICT, SIMULATE_PATH, containsString(SESSION_ID));
	}

	@Test
	void simulateReturnsConflictWhenSessionInFailedState() throws Exception {
		given(sessionService.simulate(SESSION_ID))
				.willThrow(new InvalidSessionStateException(
						"Session " + SESSION_ID + " is in FAILED state and cannot be simulated again"));

		ResultActions result = mockMvc.perform(post("/sessions/{sessionId}/simulate", SESSION_ID))
				.andExpect(status().isConflict());

		expectFullErrorResponse(result, HttpStatus.CONFLICT, SIMULATE_PATH, containsString("FAILED"));
	}

	@Test
	void simulateReturnsBadGatewayWhenEngineFails() throws Exception {
		given(sessionService.simulate(SESSION_ID))
				.willThrow(new GameEngineCommunicationException("Connection refused"));

		ResultActions result = mockMvc.perform(post("/sessions/{sessionId}/simulate", SESSION_ID))
				.andExpect(status().isBadGateway());

		expectFullErrorResponse(result, HttpStatus.BAD_GATEWAY, SIMULATE_PATH,
				containsString("Connection refused"));
	}

	private static GameSession createTestSession() {
		GameStateDto initialState = new GameStateDto(
				GAME_ID,
				Collections.nCopies(9, (String) null),
				GameStatus.NEW,
				null,
				null);
		GameSession session = new GameSession(SESSION_ID, GAME_ID);
		session.setCurrentGameState(initialState);
		return session;
	}

	private static GameSession createCompletedSession() {
		List<String> board = Arrays.asList("X", "O", "X", "O", "X", "O", null, null, "X");
		GameStateDto finalState = new GameStateDto(
				GAME_ID, board, GameStatus.X_WON, Player.X, null);
		GameSession session = new GameSession(SESSION_ID, GAME_ID);
		session.setStatus(SessionStatus.COMPLETED);
		session.setCurrentGameState(finalState);
		session.addMove(new MoveRecord(1, Player.X, 4, GameStatus.IN_PROGRESS,
				List.of("", "", "", "", "X", "", "", "", ""), Instant.now()));
		return session;
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
