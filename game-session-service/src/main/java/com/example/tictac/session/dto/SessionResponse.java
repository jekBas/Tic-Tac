package com.example.tictac.session.dto;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.model.enums.SessionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionResponse(
		String sessionId,
		String gameId,
		SessionStatus status,
		GameStateDto currentGameState,
		List<MoveRecordResponse> moveHistory,
		Instant createdAt,
		Instant updatedAt,
		String failureReason
) {

	public static SessionResponse from(GameSession session) {
		List<MoveRecordResponse> history = session.getMoveHistory().stream()
				.map(MoveRecordResponse::from)
				.toList();
		return new SessionResponse(
				session.getSessionId(),
				session.getGameId(),
				session.getStatus(),
				session.getCurrentGameState(),
				history,
				session.getCreatedAt(),
				session.getUpdatedAt(),
				session.getFailureReason());
	}
}
