package com.example.tictac.engine.repository;

import com.example.tictac.engine.model.Game;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

/**
 * Thin wrapper around a {@link ConcurrentHashMap} that owns all in-memory game
 * storage. Atomic creation is exposed via {@link #computeIfAbsent} so callers
 * can implement get-or-create semantics without races.
 */
@Repository
public class GameRepository {

	private final ConcurrentMap<String, Game> games = new ConcurrentHashMap<>();

	public Optional<Game> findById(String gameId) {
		return Optional.ofNullable(games.get(gameId));
	}

	public Game save(Game game) {
		games.put(game.getGameId(), game);
		return game;
	}

	public Game computeIfAbsent(String gameId,
															Function<String, Game> factory) {
		return games.computeIfAbsent(gameId, factory);
	}

	public boolean existsById(String gameId) {
		return games.containsKey(gameId);
	}
}
