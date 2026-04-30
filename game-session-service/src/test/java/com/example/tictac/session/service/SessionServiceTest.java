package com.example.tictac.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tictac.session.client.GameEngineClient;
import com.example.tictac.session.config.SimulationProperties;
import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.repository.SessionRepository;
import com.example.tictac.common.enums.GameStatus;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

	@Mock
	private SessionRepository sessionRepository;

	@Mock
	private GameEngineClient gameEngineClient;

	@Mock
	private SimulationStrategy simulationStrategy;

	@Mock
	private SimulationProperties simulationProperties;

	@InjectMocks
	private SessionService sessionService;

	@Test
	void createSessionCallsEngineStoresGameIdAndInitialState() {
		String engineGameId = "engine-game-id";
		GameStateDto initialState = new GameStateDto(
				engineGameId,
				Collections.nCopies(9, (String) null),
				GameStatus.NEW,
				null,
				null);
		when(gameEngineClient.createGame()).thenReturn(initialState);
		when(sessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

		GameSession session = sessionService.createSession();

		assertThat(session.getSessionId()).isNotBlank().isNotEqualTo(engineGameId);
		assertThat(session.getGameId()).isEqualTo(engineGameId);
		assertThat(session.getCurrentGameState()).isEqualTo(initialState);
		verify(sessionRepository).save(argThat(s ->
				engineGameId.equals(s.getGameId())
						&& s.getSessionId() != null
						&& initialState.equals(s.getCurrentGameState())));
	}
}
