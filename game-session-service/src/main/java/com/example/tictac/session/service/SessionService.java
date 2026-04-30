package com.example.tictac.session.service;

import com.example.tictac.common.GameConstants;
import com.example.tictac.session.client.GameEngineClient;
import com.example.tictac.session.config.SimulationProperties;
import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.dto.MoveRequest;
import com.example.tictac.session.exception.GameEngineCommunicationException;
import com.example.tictac.session.exception.InvalidSessionStateException;
import com.example.tictac.session.exception.SessionNotFoundException;
import com.example.tictac.session.exception.SimulationAlreadyRunningException;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.model.MoveRecord;
import com.example.tictac.common.enums.Player;
import com.example.tictac.session.model.enums.SessionStatus;
import com.example.tictac.session.repository.SessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestration service that owns the lifecycle of a {@link GameSession}.
 * <p>The Game Engine Service remains the source of truth for board state,
 * move validation and outcome. Session creation registers a game via
 * {@code POST /games}; simulation generates candidate moves, forwards them to
 * the engine and records the engine's responses.
 */
@Service
public class SessionService {
	private static final Logger log = LoggerFactory.getLogger(SessionService.class);

	private final SessionRepository sessionRepository;
	private final GameEngineClient gameEngineClient;
	private final SimulationStrategy simulationStrategy;
	private final SimulationProperties simulationProperties;

	public SessionService(SessionRepository sessionRepository,
												GameEngineClient gameEngineClient,
												SimulationStrategy simulationStrategy,
												SimulationProperties simulationProperties) {
		this.sessionRepository = sessionRepository;
		this.gameEngineClient = gameEngineClient;
		this.simulationStrategy = simulationStrategy;
		this.simulationProperties = simulationProperties;
	}

	/**
	 * Creates a new session by calling the Game Engine.
	 * The engine-assigned gameId is stored on the session.
	 */
	public GameSession createSession() {
		String sessionId = UUID.randomUUID().toString();
		GameStateDto initialState = gameEngineClient.createGame();
		GameSession session = new GameSession(sessionId, initialState.gameId());
		session.setCurrentGameState(initialState);
		GameSession newGameSession = sessionRepository.save(session);
		log.info("Created session {} (gameId={})", sessionId, session.getGameId());
		return newGameSession;
	}

	public GameSession getSession(String sessionId) {
		return sessionRepository.findById(sessionId)
				.orElseThrow(() -> new SessionNotFoundException(sessionId));
	}

	/**
	 * Run the simulation for the given session against the Game Engine.
	 *
	 * <p>If the session is already {@link SessionStatus#COMPLETED} the current
	 * details are returned without contacting the engine. A concurrent
	 * simulation request for the same session yields a 409 via
	 * {@link SimulationAlreadyRunningException}.
	 */
	public GameSession simulate(String sessionId) {
		GameSession session = getSession(sessionId);

		if (session.getStatus() == SessionStatus.COMPLETED) {
			return session;
		}
		if (session.getStatus() == SessionStatus.FAILED) {
			throw new InvalidSessionStateException(
					"Session " + sessionId + " is in FAILED state and cannot be simulated again");
		}

		ReentrantLock lock = session.lock();
		if (!lock.tryLock()) {
			throw new SimulationAlreadyRunningException(sessionId);
		}
		try {
			return simulateLocked(session);
		} finally {
			lock.unlock();
		}
	}

	private GameSession simulateLocked(GameSession session) {
		session.setStatus(SessionStatus.SIMULATING);

		try {
			runSimulationLoop(session);
			session.setStatus(SessionStatus.COMPLETED);
			log.info("Session {} completed with status {}",
					session.getSessionId(),
					session.getCurrentGameState() != null ? session.getCurrentGameState().status() : "unknown");
		} catch (GameEngineCommunicationException ex) {
			session.setFailureReason(ex.getMessage());
			session.setStatus(SessionStatus.FAILED);
			log.warn("Session {} failed: {}", session.getSessionId(), ex.getMessage());
			throw ex;
		} catch (RuntimeException ex) {
			session.setFailureReason(ex.getMessage());
			session.setStatus(SessionStatus.FAILED);
			log.warn("Session {} failed unexpectedly: {}", session.getSessionId(), ex.getMessage());
			throw ex;
		} finally {
			// Not strictly necessary with the current in-memory ConcurrentHashMap repository,
			// but ensures correctness if the repository is replaced with a persistent store in the future.
			sessionRepository.save(session);
		}
		return session;
	}

	private void runSimulationLoop(GameSession session) {
		Duration moveDelay = simulationProperties.moveDelay();
		String gameId = session.getGameId();

		for (int safetyCounter = 0; safetyCounter < GameConstants.BOARD_SIZE; safetyCounter++) {
			GameStateDto currentState = session.getCurrentGameState();

			if (currentState != null && currentState.status() != null && currentState.status().isTerminal()) {
				return;
			}

			Player nextPlayer = determineNextPlayer(currentState);
			List<String> board = currentState != null ? currentState.board() : emptyBoard();

			int position = simulationStrategy.chooseMove(board, nextPlayer);

			sleepBetweenMoves(moveDelay);

			GameStateDto engineState = gameEngineClient.applyMove(
					gameId, new MoveRequest(nextPlayer, position));

			session.setCurrentGameState(engineState);
			session.addMove(new MoveRecord(
					session.nextMoveNumber(),
					nextPlayer,
					position,
					engineState.status(),
					engineState.board(),
					Instant.now()));

			if (engineState.status() != null && engineState.status().isTerminal()) {
				return;
			}
		}
		throw new IllegalStateException(
				"Simulation exceeded the maximum number of moves without a terminal state");
	}

	private static Player determineNextPlayer(GameStateDto currentState) {
		if (currentState == null || currentState.nextPlayer() == null) {
			// No state yet (engine hasn't been called) or fresh game: X starts.
			return Player.X;
		}
		return currentState.nextPlayer();
	}

	private static List<String> emptyBoard() {
		return java.util.Arrays.asList(new String[9]);
	}

	private static void sleepBetweenMoves(Duration delay) {
		long millis = delay.toMillis();
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Simulation interrupted", ex);
		}
	}
}
