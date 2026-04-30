package com.example.tictac.session.model;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.session.model.enums.SessionStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory representation of a single game session orchestrated by the
 * Game Session Service. Mutating operations should be performed while holding
 * {@link #lock()} to keep status transitions and move-history updates consistent.
 *
 * <p>The move history uses a {@link CopyOnWriteArrayList} so that concurrent
 * read access (e.g. a {@code GET /sessions/{id}} in flight while a simulation
 * is running) is always safe.
 */
public class GameSession {

	private final String sessionId;
	private final String gameId;
	private final Instant createdAt;
	private final List<MoveRecord> moveHistory = new CopyOnWriteArrayList<>();
	private final ReentrantLock lock = new ReentrantLock();

	private volatile SessionStatus status;
	private volatile GameStateDto currentGameState;
	private volatile Instant updatedAt;
	private volatile String failureReason;

	public GameSession(String sessionId,
										 String gameId) {
		this.sessionId = sessionId;
		this.gameId = gameId;
		this.status = SessionStatus.CREATED;
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getGameId() {
		return gameId;
	}

	public SessionStatus getStatus() {
		return status;
	}

	public void setStatus(SessionStatus status) {
		this.status = status;
		touch();
	}

	public GameStateDto getCurrentGameState() {
		return currentGameState;
	}

	public void setCurrentGameState(GameStateDto currentGameState) {
		this.currentGameState = currentGameState;
		touch();
	}

	public List<MoveRecord> getMoveHistory() {
		return Collections.unmodifiableList(moveHistory);
	}

	public void addMove(MoveRecord record) {
		moveHistory.add(record);
		touch();
	}

	public int nextMoveNumber() {
		return moveHistory.size() + 1;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
		touch();
	}

	public ReentrantLock lock() {
		return lock;
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}
}
