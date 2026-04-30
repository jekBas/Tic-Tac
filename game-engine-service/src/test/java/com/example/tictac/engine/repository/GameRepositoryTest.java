package com.example.tictac.engine.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tictac.engine.model.Game;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameRepositoryTest {

	private GameRepository repository;

	@BeforeEach
	void setUp() {
		repository = new GameRepository();
	}

	@Test
	void findByIdReturnsEmptyWhenMissing() {
		Optional<Game> result = repository.findById("unknown");

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdReturnsGameAfterSave() {
		Game game = new Game("g1");
		repository.save(game);

		Optional<Game> result = repository.findById("g1");

		assertThat(result).containsSame(game);
	}

	@Test
	void saveReturnsSameInstance() {
		Game game = new Game("g1");

		Game returned = repository.save(game);

		assertThat(returned).isSameAs(game);
	}

	@Test
	void saveOverwritesExistingGameId() {
		Game first = new Game("g1");
		Game second = new Game("g1");
		repository.save(first);
		repository.save(second);

		Optional<Game> result = repository.findById("g1");

		assertThat(result).containsSame(second);
	}

	@Test
	void existsByIdFalseWhenAbsent() {
		assertThat(repository.existsById("missing")).isFalse();
	}

	@Test
	void existsByIdTrueAfterSave() {
		repository.save(new Game("g1"));

		assertThat(repository.existsById("g1")).isTrue();
	}

	@Test
	void computeIfAbsentInvokesFactoryOnceAndStoresGame() {
		AtomicInteger factoryCalls = new AtomicInteger();

		Game created = repository.computeIfAbsent("g1", id -> {
			factoryCalls.incrementAndGet();
			return new Game(id);
		});

		assertThat(factoryCalls).hasValue(1);
		assertThat(created.getGameId()).isEqualTo("g1");
		assertThat(repository.findById("g1")).containsSame(created);
	}

	@Test
	void computeIfAbsentDoesNotInvokeFactoryWhenKeyPresent() {
		Game existing = new Game("g1");
		repository.save(existing);
		AtomicInteger factoryCalls = new AtomicInteger();

		Game result = repository.computeIfAbsent("g1", id -> {
			factoryCalls.incrementAndGet();
			return new Game(id);
		});

		assertThat(factoryCalls).hasValue(0);
		assertThat(result).isSameAs(existing);
	}
}
