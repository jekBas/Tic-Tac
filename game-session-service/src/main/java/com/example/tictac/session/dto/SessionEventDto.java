package com.example.tictac.session.dto;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.model.MoveRecord;
import com.example.tictac.session.model.enums.SessionStatus;
import java.util.List;

public record SessionEventDto(
		String sessionId,
		String gameId,
		SessionStatus sessionStatus,
		GameStateDto currentGameState,
		MoveRecordResponse latestMove,
		List<MoveRecordResponse> moveHistory,
		String failureReason
) {

	public static SessionEventDto from(GameSession session) {
		List<MoveRecordResponse> history = session.getMoveHistory().stream()
				.map(MoveRecordResponse::from)
				.toList();
		MoveRecordResponse latest = history.isEmpty() ? null : history.getLast();
		return new SessionEventDto(
				session.getSessionId(),
				session.getGameId(),
				session.getStatus(),
				session.getCurrentGameState(),
				latest,
				history,
				session.getFailureReason());
	}
}