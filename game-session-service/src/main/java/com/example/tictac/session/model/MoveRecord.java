package com.example.tictac.session.model;

import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
		// Empty cells arrive as JSON null from the Game Engine and stay null in
		// Java; List.copyOf rejects null elements, so use an unmodifiable
		// ArrayList copy instead.
		boardAfterMove = boardAfterMove == null
				? List.of()
				: Collections.unmodifiableList(new ArrayList<>(boardAfterMove));
	}
}
