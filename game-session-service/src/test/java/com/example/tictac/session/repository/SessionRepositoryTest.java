package com.example.tictac.session.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tictac.session.model.GameSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionRepositoryTest {

	private SessionRepository repository;

	@BeforeEach
	void setUp() {
		repository = new SessionRepository();
	}

	@Test
	void saveAndFindByIdReturnsSession() {
		GameSession session = new GameSession("session-1", "game-1");

		repository.save(session);

		Optional<GameSession> found = repository.findById("session-1");
		assertThat(found).isPresent().containsSame(session);
	}

	@Test
	void findByIdReturnsEmptyWhenNotFound() {
		Optional<GameSession> found = repository.findById("non-existent");

		assertThat(found).isEmpty();
	}

	@Test
	void saveOverwritesExistingSession() {
		GameSession original = new GameSession("session-1", "game-1");
		GameSession replacement = new GameSession("session-1", "game-2");

		repository.save(original);
		repository.save(replacement);

		Optional<GameSession> found = repository.findById("session-1");
		assertThat(found).isPresent().containsSame(replacement);
	}

	@Test
	void saveReturnsTheSameSession() {
		GameSession session = new GameSession("session-1", "game-1");

		GameSession returned = repository.save(session);

		assertThat(returned).isSameAs(session);
	}

	@Test
	void multipleSessions() {
		GameSession session1 = new GameSession("session-1", "game-1");
		GameSession session2 = new GameSession("session-2", "game-2");

		repository.save(session1);
		repository.save(session2);

		assertThat(repository.findById("session-1")).isPresent().containsSame(session1);
		assertThat(repository.findById("session-2")).isPresent().containsSame(session2);
	}
}
