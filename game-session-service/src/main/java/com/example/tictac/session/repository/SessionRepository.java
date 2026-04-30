package com.example.tictac.session.repository;

import com.example.tictac.session.model.GameSession;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

/**
 * Thin wrapper around a {@link ConcurrentHashMap} that owns all in-memory
 * {@link GameSession} storage.
 */
@Repository
public class SessionRepository {

	private final ConcurrentMap<String, GameSession> sessions = new ConcurrentHashMap<>();

	public Optional<GameSession> findById(String sessionId) {
		return Optional.ofNullable(sessions.get(sessionId));
	}

	public GameSession save(GameSession session) {
		sessions.put(session.getSessionId(), session);
		return session;
	}
}
