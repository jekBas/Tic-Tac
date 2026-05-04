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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameService {
	private static final Logger log = LoggerFactory.getLogger(GameService.class);

	private final GameRepository gameRepository;

	// Constructor.
	public GameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	public Game createGame() {
		Game game = gameRepository.save(new Game(UUID.randomUUID().toString()));
		log.info("Created game {}", game.getGameId());
		return game;
	}

	public Game getGame(String gameId) {
		log.debug("Retrieving game {}", gameId);
		return gameRepository.findById(gameId)
				.orElseThrow(() -> {
					log.error("Game {} not found", gameId);
					return new GameNotFoundException(gameId);
				});
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
			log.debug("Applying move: game={}, player={}, position={}", gameId, request.player(), request.position());
			applyMoveLocked(game, request.player(), request.position());
			Game saved = gameRepository.save(game);
			log.info("Move applied: game={}, player={}, position={}, status={}",
					gameId, request.player(), request.position(), saved.getStatus());
			return saved;
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

	private void validateGame(Game game, Player player) {
		if (game.isTerminal()) {
			log.error("Rejected move on finished game {}: status={}", game.getGameId(), game.getStatus());
			throw new InvalidMoveException(
					"Game is already finished with status " + game.getStatus());
		}

		if (game.getStatus() == GameStatus.IN_PROGRESS && player != game.getNextPlayer()) {
			log.error("Rejected out-of-turn move on game {}: player={}, expected={}", game.getGameId(), player, game.getNextPlayer());
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
