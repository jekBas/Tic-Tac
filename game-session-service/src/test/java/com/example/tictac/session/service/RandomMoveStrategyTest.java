package com.example.tictac.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RandomMoveStrategyTest {

	private final RandomMoveStrategy strategy = new RandomMoveStrategy();

	@Test
	void choosesOnlyEmptyCells() {
		List<String> board = Arrays.asList("X", null, "O", null, "X", null, "O", null, null);
		Set<Integer> emptyCells = Set.of(1, 3, 5, 7, 8);

		for (int i = 0; i < 100; i++) {
			int move = strategy.chooseMove(board);
			assertThat(move).isIn(emptyCells.toArray());
		}
	}

	@Test
	void choosesTheOnlyEmptyCell() {
		List<String> board = Arrays.asList("X", "O", "X", "O", "X", "O", "O", "X", null);

		int move = strategy.chooseMove(board);

		assertThat(move).isEqualTo(8);
	}

	@Test
	void coversAllEmptyCellsOverManyRuns() {
		List<String> board = Arrays.asList("X", null, null, null, "O", null, null, null, null);
		Set<Integer> emptyCells = Set.of(1, 2, 3, 5, 6, 7, 8);
		Set<Integer> chosen = new HashSet<>();

		for (int i = 0; i < 1000; i++) {
			chosen.add(strategy.chooseMove(board));
		}

		assertThat(chosen).containsExactlyInAnyOrderElementsOf(emptyCells);
	}

	@Test
	void usesProvidedRandomForDeterministicBehavior() {
		Random seededRandom = new Random(42);
		RandomMoveStrategy seeded = new RandomMoveStrategy(seededRandom);
		List<String> board = Arrays.asList(null, null, null, null, null, null, null, null, null);

		int move = seeded.chooseMove(board);

		assertThat(move).isBetween(0, 8);
	}

	@Test
	void throwsOnFullBoard() {
		List<String> board = Arrays.asList("X", "O", "X", "O", "X", "O", "O", "X", "O");

		assertThatThrownBy(() -> strategy.chooseMove(board))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No empty cell");
	}

	@Test
	void throwsOnNullBoard() {
		assertThatThrownBy(() -> strategy.chooseMove(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("invalid board");
	}

	@Test
	void throwsOnWrongSizeBoard() {
		List<String> board = Arrays.asList("X", "O", null);

		assertThatThrownBy(() -> strategy.chooseMove(board))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("invalid board");
	}
}