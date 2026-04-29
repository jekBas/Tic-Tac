package com.example.tictac.engine.dto;

import com.example.tictac.engine.model.Game;
import com.example.tictac.engine.model.enums.GameStatus;
import com.example.tictac.engine.model.enums.Player;
import java.util.Arrays;
import java.util.List;

public record GameResponse(
		String gameId,
		List<String> board,
		GameStatus status,
		Player winner,
		Player nextPlayer
) {

	public static GameResponse from(Game game) {

		List<String> cells = Arrays.stream(game.getBoard())
				.map(cell -> cell == null ? null : cell.name())
				.toList();

		Player nextPlayer = game.getStatus() == GameStatus.IN_PROGRESS ? game.getNextPlayer() : null;

		return new GameResponse(
				game.getGameId(),
				cells,
				game.getStatus(),
				game.getWinner(),
				nextPlayer);
	}
}
