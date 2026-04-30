package com.example.tictac.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tictac.common.enums.Player;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SimulationStrategyTest {

	private final SimulationStrategy strategy = new SimulationStrategy();

	@ParameterizedTest(name = "wins at position {2}: {0}")
	@MethodSource("winningMoveScenarios")
	void takesWinningMoveWhenAvailable(String scenario, Player player, int expectedPosition, List<String> board) {
		int move = strategy.chooseMove(board, player);

		assertThat(move).isEqualTo(expectedPosition);
	}

	static Stream<Arguments> winningMoveScenarios() {
		return Stream.of(
				Arguments.of("X completes row", Player.X, 2,
						Arrays.asList("X", "X", null, "O", "O", null, null, null, null)),
				Arguments.of("O completes row", Player.O, 2,
						Arrays.asList("O", "O", null, "X", "X", null, null, null, null)),
				Arguments.of("X completes column", Player.X, 6,
						Arrays.asList("X", "O", null, "X", "O", null, null, null, null)),
				Arguments.of("X completes diagonal", Player.X, 8,
						Arrays.asList("X", "O", null, null, "X", null, "O", null, null))
		);
	}

	@ParameterizedTest(name = "blocks at position {2}: {0}")
	@MethodSource("blockingMoveScenarios")
	void blocksOpponentWinningMove(String scenario, Player player, int expectedPosition, List<String> board) {
		int move = strategy.chooseMove(board, player);

		assertThat(move).isEqualTo(expectedPosition);
	}

	static Stream<Arguments> blockingMoveScenarios() {
		return Stream.of(
				Arguments.of("blocks row", Player.X, 2,
						Arrays.asList("O", "O", null, "X", null, null, null, null, null)),
				Arguments.of("blocks column", Player.X, 6,
						Arrays.asList("O", null, null, "O", "X", null, null, null, null)),
				Arguments.of("blocks diagonal", Player.X, 8,
						Arrays.asList("O", null, null, null, "O", null, "X", null, null))
		);
	}

	@Test
	void prefersWinningOverBlocking() {
		List<String> board = Arrays.asList(
				"X", "X", null,
				"O", "O", null,
				null, null, null);

		int move = strategy.chooseMove(board, Player.X);

		assertThat(move).isEqualTo(2);
	}

	@ParameterizedTest(name = "fallback priority: {0}")
	@MethodSource("fallbackPriorityScenarios")
	void fallsBackToPositionalPriority(String scenario, Player player, List<Integer> expectedPositions, List<String> board) {
		int move = strategy.chooseMove(board, player);

		assertThat(move).isIn(expectedPositions.toArray());
	}

	static Stream<Arguments> fallbackPriorityScenarios() {
		return Stream.of(
				Arguments.of("takes center on empty board", Player.X,
						List.of(4),
						Arrays.asList(null, null, null, null, null, null, null, null, null)),
				Arguments.of("takes center when available", Player.X,
						List.of(4),
						Arrays.asList("X", null, null, null, null, null, null, null, "O")),
				Arguments.of("takes corner when center occupied", Player.X,
						List.of(0, 2, 6, 8),
						Arrays.asList(null, null, null, null, "O", null, null, null, null)),
				Arguments.of("takes side when corners and center occupied", Player.O,
						List.of(1, 3, 5, 7),
						Arrays.asList("X", null, "O", null, "X", null, "O", null, "X")),
				Arguments.of("takes last available cell", Player.O,
						List.of(8),
						Arrays.asList("X", "O", "X", "O", "X", "O", "O", "X", null))
		);
	}

	@ParameterizedTest(name = "invalid board: {0}")
	@MethodSource("invalidBoardScenarios")
	void throwsOnInvalidBoard(String scenario, List<String> board) {
		assertThatThrownBy(() -> strategy.chooseMove(board, Player.X))
				.isInstanceOf(IllegalStateException.class);
	}

	static Stream<Arguments> invalidBoardScenarios() {
		return Stream.of(
				Arguments.of("null board", null),
				Arguments.of("too small", Arrays.asList("X", "O", null)),
				Arguments.of("too large", Arrays.asList(null, null, null, null, null, null, null, null, null, null)),
				Arguments.of("full board", Arrays.asList("X", "O", "X", "O", "X", "O", "O", "X", "O"))
		);
	}
}
