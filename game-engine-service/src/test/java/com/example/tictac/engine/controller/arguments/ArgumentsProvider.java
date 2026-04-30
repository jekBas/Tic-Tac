package com.example.tictac.engine.controller.arguments;

import com.example.tictac.common.GameConstants;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/**
 * Supplies move {@code position} cases for {@link com.example.tictac.engine.controller.GameControllerTest}
 * parameterized tests. Dispatches by the executing test method name.
 */
class MovePositionArgumentsProvider implements ArgumentsProvider {

	private static final String INVALID_POSITIONS_TEST = "applyMoveRejectsInvalidPositions";
	private static final String VALID_POSITIONS_TEST = "applyMoveAcceptsValidPositions";

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		return switch (context.getRequiredTestMethod().getName()) {
			case INVALID_POSITIONS_TEST -> invalidPositions();
			case VALID_POSITIONS_TEST -> validPositions();
			default -> throw new IllegalStateException(
					"MovePositionArgumentsProvider is only for " + INVALID_POSITIONS_TEST + " / "
							+ VALID_POSITIONS_TEST + "; got: " + context.getRequiredTestMethod().getName());
		};
	}

	private static Stream<Arguments> invalidPositions() {
		return Stream.of(
				Arguments.of((Integer) null),
				Arguments.of(-1),
				Arguments.of(-100),
				Arguments.of(GameConstants.BOARD_SIZE),
				Arguments.of(GameConstants.BOARD_SIZE + 1),
				Arguments.of(100));
	}

	private static Stream<Arguments> validPositions() {
		return Stream.of(
				Arguments.of(0),
				Arguments.of(1),
				Arguments.of(4),
				Arguments.of(GameConstants.BOARD_SIZE - 1));
	}
}
