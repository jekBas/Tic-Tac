package com.example.tictac.session.model;

import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import java.time.Instant;
import java.util.List;

public record MoveRecord(
		int moveNumber,
		Player player,
		int position,
		GameStatus resultingStatus,
		List<String> boardAfterMove,
		Instant createdAt
) {

	public MoveRecord {
		boardAfterMove = boardAfterMove == null ? List.of() : List.copyOf(boardAfterMove);
	}
}
