package com.example.tictac.session.dto;

import com.example.tictac.session.model.MoveRecord;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import java.time.Instant;
import java.util.List;

public record MoveRecordResponse(
		int moveNumber,
		Player player,
		int position,
		GameStatus resultingStatus,
		List<String> boardAfterMove,
		Instant createdAt
) {

	public static MoveRecordResponse from(MoveRecord record) {
		return new MoveRecordResponse(
				record.moveNumber(),
				record.player(),
				record.position(),
				record.resultingStatus(),
				record.boardAfterMove(),
				record.createdAt());
	}
}
