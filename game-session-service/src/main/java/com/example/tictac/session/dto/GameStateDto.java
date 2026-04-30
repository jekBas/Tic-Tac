package com.example.tictac.session.dto;

import com.example.tictac.session.model.enums.GameStatus;
import com.example.tictac.session.model.enums.Player;
import java.util.List;

/**
 * Game state representation as returned by the Game Engine Service.
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
