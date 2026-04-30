package com.example.tictac.engine.service;

import com.example.tictac.common.dto.MoveRequest;
import com.example.tictac.engine.exception.GameNotFoundException;
import com.example.tictac.engine.exception.InvalidMoveException;
import com.example.tictac.engine.model.Game;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import com.example.tictac.common.enums.WinningLine;
import com.example.tictac.engine.repository.GameRepository;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

@Service
public class GameService {

	private final GameRepository gameRepository;

	// Constructor.
	public GameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	public Game createGame() {
		return gameRepository.save(new Game(UUID.randomUUID().toString()));
	}

	public Game getGame(String gameId) {
		return gameRepository.findById(gameId)
				.orElseThrow(() -> new GameNotFoundException(gameId));
	}

	/**
	 * Apply a move, creating the game on demand if it does not exist yet.
	 * Per-game locking guarantees serialized rule evaluation while different
	 * games can still progress in parallel.
	 */
	public Game applyMove(String gameId,
												MoveRequest request) {
		Game game = gameRepository.computeIfAbsent(gameId, Game::new);
		ReentrantLock lock = game.lock();
		lock.lock();
		try {
			applyMoveLocked(game, request.player(), request.position());
			// Not required for the current in-memory repository, but keeps the write
			// flow explicit for a future migration to database-backed persistence.
			return gameRepository.save(game);
		} finally {
			lock.unlock();
		}
	}

	private void applyMoveLocked(Game game,
															 Player player,
															 int position) {
		validateGame(game, player);

		Player[] board = game.getBoard();
		if (board[position] != null) {
			throw new InvalidMoveException("Cell " + position + " is already occupied");
		}

		board[position] = player;

		if (isWinningMove(board, player)) {
			game.setStatus(GameStatus.wonBy(player));
			game.setWinner(player);
			game.setNextPlayer(null);
		} else if (isBoardFull(board)) {
			game.setStatus(GameStatus.DRAW);
			game.setNextPlayer(null);
		} else {
			game.setStatus(GameStatus.IN_PROGRESS);
			game.setNextPlayer(player.opposite());
		}
	}

	private static void validateGame(Game game, Player player) {
		if (game.isTerminal()) {
			throw new InvalidMoveException(
					"Game is already finished with status " + game.getStatus());
		}

		if (player != game.getNextPlayer()) {
			throw new InvalidMoveException(
					"It is not " + player + "'s turn; expected " + game.getNextPlayer());
		}
	}

	private boolean isWinningMove(Player[] board, Player player) {
		for (WinningLine line : WinningLine.values()) {
			if (line.isClaimedBy(board, player)) {
				return true;
			}
		}
		return false;
	}

	private boolean isBoardFull(Player[] board) {
		for (Player cell : board) {
			if (cell == null) {
				return false;
			}
		}
		return true;
	}
}
