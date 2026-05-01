package com.example.tictac.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import com.example.tictac.session.dto.SessionEventDto;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.model.MoveRecord;
import com.example.tictac.session.model.enums.SessionStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class SessionEventPublisherTest {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private SessionEventPublisher publisher;

	@Captor
	private ArgumentCaptor<SessionEventDto> eventCaptor;

	@Test
	void publishUpdateSendsToCorrectDestination() {
		GameSession session = new GameSession("session-123", "game-456");
		session.setStatus(SessionStatus.SIMULATING);

		publisher.publishUpdate(session);

		verify(messagingTemplate).convertAndSend(
				eq("/topic/sessions/session-123"), eventCaptor.capture());
		SessionEventDto event = eventCaptor.getValue();
		assertThat(event.sessionId()).isEqualTo("session-123");
		assertThat(event.gameId()).isEqualTo("game-456");
		assertThat(event.sessionStatus()).isEqualTo(SessionStatus.SIMULATING);
	}

	@Test
	void publishUpdateIncludesLatestMoveAndHistory() {
		GameSession session = new GameSession("session-1", "game-1");
		session.setStatus(SessionStatus.SIMULATING);
		List<String> board = java.util.Arrays.asList("X", null, null, null, null, null, null, null, null);
		GameStateDto state = new GameStateDto("game-1", board, GameStatus.IN_PROGRESS, null, Player.O);
		session.setCurrentGameState(state);
		session.addMove(new MoveRecord(1, Player.X, 0, GameStatus.IN_PROGRESS, board, Instant.now()));

		publisher.publishUpdate(session);

		verify(messagingTemplate).convertAndSend(
				eq("/topic/sessions/session-1"), eventCaptor.capture());
		SessionEventDto event = eventCaptor.getValue();
		assertThat(event.moveHistory()).hasSize(1);
		assertThat(event.latestMove()).isNotNull();
		assertThat(event.latestMove().player()).isEqualTo(Player.X);
		assertThat(event.latestMove().position()).isEqualTo(0);
		assertThat(event.currentGameState()).isEqualTo(state);
	}

	@Test
	void publishUpdateIncludesFailureReason() {
		GameSession session = new GameSession("session-1", "game-1");
		session.setStatus(SessionStatus.FAILED);
		session.setFailureReason("Connection refused");

		publisher.publishUpdate(session);

		verify(messagingTemplate).convertAndSend(
				eq("/topic/sessions/session-1"), eventCaptor.capture());
		SessionEventDto event = eventCaptor.getValue();
		assertThat(event.sessionStatus()).isEqualTo(SessionStatus.FAILED);
		assertThat(event.failureReason()).isEqualTo("Connection refused");
	}
}