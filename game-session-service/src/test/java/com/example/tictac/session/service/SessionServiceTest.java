package com.example.tictac.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.dto.MoveRequest;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import com.example.tictac.session.client.GameEngineClient;
import com.example.tictac.session.config.SimulationProperties;
import com.example.tictac.session.exception.GameEngineCommunicationException;
import com.example.tictac.session.exception.InvalidSessionStateException;
import com.example.tictac.session.exception.SessionNotFoundException;
import com.example.tictac.session.exception.SimulationAlreadyRunningException;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.model.enums.SessionStatus;
import com.example.tictac.session.repository.SessionRepository;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

	private static final String ENGINE_GAME_ID = "engine-game-id";

	@Mock
	private SessionRepository sessionRepository;

	@Mock
	private GameEngineClient gameEngineClient;

	@Mock
	private SimulationStrategy simulationStrategy;

	@Mock
	private SimulationProperties simulationProperties;

	@Mock
	private SessionEventPublisher eventPublisher;

	@InjectMocks
	private SessionService sessionService;

	@Test
	void createSessionCallsEngineStoresGameIdAndInitialState() {
		GameStateDto initialState = new GameStateDto(
				ENGINE_GAME_ID,
				Collections.nCopies(9, (String) null),
				GameStatus.NEW,
				null,
				null);
		when(gameEngineClient.createGame()).thenReturn(initialState);
		when(sessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

		GameSession session = sessionService.createSession();

		assertThat(session.getSessionId()).isNotBlank().isNotEqualTo(ENGINE_GAME_ID);
		assertThat(session.getGameId()).isEqualTo(ENGINE_GAME_ID);
		assertThat(session.getCurrentGameState()).isEqualTo(initialState);
		verify(sessionRepository).save(argThat(s ->
				ENGINE_GAME_ID.equals(s.getGameId())
						&& s.getSessionId() != null
						&& initialState.equals(s.getCurrentGameState())));
	}

	@Test
	void getSessionReturnsSessionWhenFound() {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

		GameSession result = sessionService.getSession("session-1");

		assertThat(result).isSameAs(session);
	}

	@Test
	void getSessionThrowsWhenNotFound() {
		when(sessionRepository.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> sessionService.getSession("missing"))
				.isInstanceOf(SessionNotFoundException.class)
				.hasMessageContaining("missing");
	}

	@Test
	void simulateReturnsCompletedSessionWithoutCallingEngine() {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		session.setStatus(SessionStatus.COMPLETED);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

		GameSession result = sessionService.simulate("session-1");

		assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
		verify(gameEngineClient, never()).applyMove(any(), any());
	}

	@Test
	void simulateThrowsWhenSessionInFailedState() {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		session.setStatus(SessionStatus.FAILED);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

		assertThatThrownBy(() -> sessionService.simulate("session-1"))
				.isInstanceOf(InvalidSessionStateException.class)
				.hasMessageContaining("FAILED");
	}

	@Test
	void simulateThrowsWhenAlreadyRunning() throws InterruptedException {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

		Thread lockHolder = new Thread(() -> {
			session.lock().lock();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				session.lock().unlock();
			}
		});
		lockHolder.start();
		Thread.sleep(50);

		try {
			assertThatThrownBy(() -> sessionService.simulate("session-1"))
					.isInstanceOf(SimulationAlreadyRunningException.class)
					.hasMessageContaining("session-1");
		} finally {
			lockHolder.interrupt();
			lockHolder.join();
		}
	}

	@Test
	void simulateRunsGameToTerminalState() {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		GameStateDto initialState = new GameStateDto(
				ENGINE_GAME_ID, Collections.nCopies(9, (String) null), GameStatus.NEW, null, null);
		session.setCurrentGameState(initialState);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));
		when(simulationProperties.moveDelay()).thenReturn(Duration.ZERO);
		when(simulationStrategy.chooseMove(any(), any())).thenReturn(4);

		GameStateDto terminalState = new GameStateDto(
				ENGINE_GAME_ID,
				List.of("X", "X", "X", "", "", "", "", "", ""),
				GameStatus.X_WON, Player.X, null);
		when(gameEngineClient.applyMove(eq(ENGINE_GAME_ID), any(MoveRequest.class)))
				.thenReturn(terminalState);

		GameSession result = sessionService.simulate("session-1");

		assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertThat(result.getCurrentGameState().status()).isEqualTo(GameStatus.X_WON);
		assertThat(result.getMoveHistory()).hasSize(1);
		verify(sessionRepository).save(session);
	}

	@Test
	void simulateRunsMultipleMovesUntilTerminal() {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		GameStateDto initialState = new GameStateDto(
				ENGINE_GAME_ID, Collections.nCopies(9, (String) null), GameStatus.NEW, null, null);
		session.setCurrentGameState(initialState);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));
		when(simulationProperties.moveDelay()).thenReturn(Duration.ZERO);
		when(simulationStrategy.chooseMove(any(), any())).thenReturn(4, 0, 2);

		GameStateDto move1State = new GameStateDto(
				ENGINE_GAME_ID,
				List.of("", "", "", "", "X", "", "", "", ""),
				GameStatus.IN_PROGRESS, null, Player.O);
		GameStateDto move2State = new GameStateDto(
				ENGINE_GAME_ID,
				List.of("O", "", "", "", "X", "", "", "", ""),
				GameStatus.IN_PROGRESS, null, Player.X);
		GameStateDto move3State = new GameStateDto(
				ENGINE_GAME_ID,
				List.of("O", "", "X", "", "X", "", "", "", ""),
				GameStatus.X_WON, Player.X, null);
		when(gameEngineClient.applyMove(eq(ENGINE_GAME_ID), any(MoveRequest.class)))
				.thenReturn(move1State, move2State, move3State);

		GameSession result = sessionService.simulate("session-1");

		assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertThat(result.getMoveHistory()).hasSize(3);
	}

	@Test
	void simulateSetsFailedStatusOnEngineCommunicationError() {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		GameStateDto initialState = new GameStateDto(
				ENGINE_GAME_ID, Collections.nCopies(9, (String) null), GameStatus.NEW, null, null);
		session.setCurrentGameState(initialState);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));
		when(simulationProperties.moveDelay()).thenReturn(Duration.ZERO);
		when(simulationStrategy.chooseMove(any(), any())).thenReturn(4);
		when(gameEngineClient.applyMove(eq(ENGINE_GAME_ID), any(MoveRequest.class)))
				.thenThrow(new GameEngineCommunicationException("Connection refused"));

		assertThatThrownBy(() -> sessionService.simulate("session-1"))
				.isInstanceOf(GameEngineCommunicationException.class);

		assertThat(session.getStatus()).isEqualTo(SessionStatus.FAILED);
		assertThat(session.getFailureReason()).contains("Connection refused");
		verify(sessionRepository).save(session);
	}

	@Test
	void simulateSetsFailedStatusOnUnexpectedRuntimeException() {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		GameStateDto initialState = new GameStateDto(
				ENGINE_GAME_ID, Collections.nCopies(9, (String) null), GameStatus.NEW, null, null);
		session.setCurrentGameState(initialState);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));
		when(simulationProperties.moveDelay()).thenReturn(Duration.ZERO);
		when(simulationStrategy.chooseMove(any(), any()))
				.thenThrow(new IllegalStateException("No empty cell available on the board"));

		assertThatThrownBy(() -> sessionService.simulate("session-1"))
				.isInstanceOf(IllegalStateException.class);

		assertThat(session.getStatus()).isEqualTo(SessionStatus.FAILED);
		assertThat(session.getFailureReason()).contains("No empty cell");
		verify(sessionRepository).save(session);
	}

	@Test
	void simulateUsesNextPlayerFromGameState() {
		GameSession session = new GameSession("session-1", ENGINE_GAME_ID);
		GameStateDto stateWithNextO = new GameStateDto(
				ENGINE_GAME_ID,
				Arrays.asList(null, null, null, null, "X", null, null, null, null),
				GameStatus.IN_PROGRESS, null, Player.O);
		session.setCurrentGameState(stateWithNextO);
		when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));
		when(simulationProperties.moveDelay()).thenReturn(Duration.ZERO);
		when(simulationStrategy.chooseMove(any(), eq(Player.O))).thenReturn(0);

		GameStateDto terminalState = new GameStateDto(
				ENGINE_GAME_ID,
				List.of("O", "", "", "", "X", "", "", "", ""),
				GameStatus.DRAW, null, null);
		when(gameEngineClient.applyMove(eq(ENGINE_GAME_ID), any(MoveRequest.class)))
				.thenReturn(terminalState);

		sessionService.simulate("session-1");

		verify(simulationStrategy).chooseMove(any(), eq(Player.O));
	}
}
