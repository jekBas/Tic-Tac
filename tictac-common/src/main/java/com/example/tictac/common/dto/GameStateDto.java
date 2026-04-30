package com.example.tictac.common.dto;

import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import java.util.List;

/**
 * Serialized game state shared by the Game Engine and Game Session services.
 * <p>The {@code board} contains nine cells where each cell is either
 * {@code "X"}, {@code "O"} or {@code null}.
 */
public record GameStateDto(
		String gameId,
		List<String> board,
		GameStatus status,
		Player winner,
		Player nextPlayer
) {}
