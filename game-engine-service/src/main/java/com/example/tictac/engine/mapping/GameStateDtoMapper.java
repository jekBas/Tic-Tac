package com.example.tictac.engine.mapping;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import com.example.tictac.engine.model.Game;
import java.util.Arrays;
import java.util.List;

public final class GameStateDtoMapper {

	private GameStateDtoMapper() {
	}

	public static GameStateDto from(Game game) {
		List<String> cells = Arrays.stream(game.getBoard())
				.map(cell -> cell == null ? null : cell.name())
				.toList();

		Player nextPlayer = game.getStatus() == GameStatus.IN_PROGRESS ? game.getNextPlayer() : null;

		return new GameStateDto(
				game.getGameId(),
				cells,
				game.getStatus(),
				game.getWinner(),
				nextPlayer);
	}
}
