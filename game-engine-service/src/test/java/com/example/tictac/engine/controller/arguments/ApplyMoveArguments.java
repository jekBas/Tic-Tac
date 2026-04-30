package com.example.tictac.engine.controller.arguments;

import com.example.tictac.common.GameConstants;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

public final class ApplyMoveArguments {

	private ApplyMoveArguments() {
	}

	public static final class InvalidPositions implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			return Stream.of(
					Arguments.of((Integer) null),
					Arguments.of(-1),
					Arguments.of(-100),
					Arguments.of(GameConstants.BOARD_SIZE),
					Arguments.of(GameConstants.BOARD_SIZE + 1),
					Arguments.of(100));
		}
	}

	public static final class ValidPositions implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			return Stream.of(
					Arguments.of(0),
					Arguments.of(1),
					Arguments.of(4),
					Arguments.of(GameConstants.BOARD_SIZE - 1));
		}
	}
}
