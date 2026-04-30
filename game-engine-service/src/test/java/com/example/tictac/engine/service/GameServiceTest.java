package com.example.tictac.engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tictac.engine.dto.MoveRequest;
import com.example.tictac.engine.exception.GameNotFoundException;
import com.example.tictac.engine.exception.InvalidMoveException;
import com.example.tictac.engine.model.Game;
import com.example.tictac.engine.model.enums.GameStatus;
import com.example.tictac.engine.model.enums.Player;
import com.example.tictac.engine.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameServiceTest {

	private GameRepository repository;
	private GameService gameService;

	@BeforeEach
	void setUp() {
		repository = new GameRepository();
		gameService = new GameService(repository);
	}

	@Test
	void createGamePersistsGameWithUniqueId() {
		Game created = gameService.createGame();

		assertThat(created.getGameId()).isNotBlank();
		assertThat(repository.findById(created.getGameId())).containsSame(created);
	}

	@Test
	void getGameReturnsGameWhenPresent() {
		Game saved = repository.save(new Game("g1"));

		Game result = gameService.getGame("g1");

		assertThat(result).isSameAs(saved);
	}

	@Test
	void getGameThrowsWhenMissing() {
		assertThatThrownBy(() -> gameService.getGame("missing"))
				.isInstanceOf(GameNotFoundException.class)
				.hasMessageContaining("missing");
	}

	@Test
	void applyMoveFirstMoveCreatesGameAndAdvancesTurn() {
		String gameId = "new-game";

		Game after = gameService.applyMove(gameId, new MoveRequest(Player.X, 0));

		assertThat(after.getBoard()[0]).isEqualTo(Player.X);
		assertThat(after.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
		assertThat(after.getNextPlayer()).isEqualTo(Player.O);
		assertThat(repository.findById(gameId)).containsSame(after);
	}

	@Test
	void applyMoveRejectsWrongPlayer() {
		String gameId = "g1";

		assertThatThrownBy(() -> gameService.applyMove(gameId, new MoveRequest(Player.O, 0)))
				.isInstanceOf(InvalidMoveException.class)
				.hasMessageContaining("not O's turn");
	}

	@Test
	void applyMoveRejectsOccupiedCell() {
		String gameId = "g1";
		gameService.applyMove(gameId, new MoveRequest(Player.X, 0));

		assertThatThrownBy(() -> gameService.applyMove(gameId, new MoveRequest(Player.O, 0)))
				.isInstanceOf(InvalidMoveException.class)
				.hasMessageContaining("already occupied");
	}

	@Test
	void applyMoveDetectsWin() {
		String gameId = "g1";
		gameService.applyMove(gameId, new MoveRequest(Player.X, 0));
		gameService.applyMove(gameId, new MoveRequest(Player.O, 3));
		gameService.applyMove(gameId, new MoveRequest(Player.X, 1));
		gameService.applyMove(gameId, new MoveRequest(Player.O, 4));

		Game finished = gameService.applyMove(gameId, new MoveRequest(Player.X, 2));

		assertThat(finished.getStatus()).isEqualTo(GameStatus.X_WON);
		assertThat(finished.getWinner()).isEqualTo(Player.X);
		assertThat(finished.getNextPlayer()).isEqualTo(Player.X);
	}

	@Test
	void applyMoveDetectsDraw() {
		String gameId = "g1";
		gameService.applyMove(gameId, new MoveRequest(Player.X, 0));
		gameService.applyMove(gameId, new MoveRequest(Player.O, 1));
		gameService.applyMove(gameId, new MoveRequest(Player.X, 2));
		gameService.applyMove(gameId, new MoveRequest(Player.O, 4));
		gameService.applyMove(gameId, new MoveRequest(Player.X, 3));
		gameService.applyMove(gameId, new MoveRequest(Player.O, 5));
		gameService.applyMove(gameId, new MoveRequest(Player.X, 7));
		gameService.applyMove(gameId, new MoveRequest(Player.O, 6));

		Game finished = gameService.applyMove(gameId, new MoveRequest(Player.X, 8));

		assertThat(finished.getStatus()).isEqualTo(GameStatus.DRAW);
		assertThat(finished.getWinner()).isNull();
	}

	@Test
	void applyMoveRejectsMovesAfterGameFinished() {
		String gameId = "g1";
		gameService.applyMove(gameId, new MoveRequest(Player.X, 0));
		gameService.applyMove(gameId, new MoveRequest(Player.O, 3));
		gameService.applyMove(gameId, new MoveRequest(Player.X, 1));
		gameService.applyMove(gameId, new MoveRequest(Player.O, 4));
		gameService.applyMove(gameId, new MoveRequest(Player.X, 2));

		assertThatThrownBy(() -> gameService.applyMove(gameId, new MoveRequest(Player.O, 5)))
				.isInstanceOf(InvalidMoveException.class)
				.hasMessageContaining("already finished");
	}
}
