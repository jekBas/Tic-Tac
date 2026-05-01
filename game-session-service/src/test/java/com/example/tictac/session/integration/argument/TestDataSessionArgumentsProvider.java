package com.example.tictac.session.integration.argument;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.springframework.http.HttpStatus;

/**
 * Grouped {@link ArgumentsProvider} implementations for session integration tests.
 */
public final class TestDataSessionArgumentsProvider {

	private TestDataSessionArgumentsProvider() {}

	// ------------------------------------------------------------------
	// POST /sessions
	// ------------------------------------------------------------------
	public static class CreateSession implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext ctx) {
			return Stream.of(
					Arguments.of(
							"Engine returns initial NEW state -> 201 CREATED",
							"create-session/engine-new-state-201/engine-create-game.json",
							"create-session/engine-new-state-201/expected-session-created.json",
							HttpStatus.CREATED));
		}
	}

	// ------------------------------------------------------------------
	// GET /sessions/{id}
	// ------------------------------------------------------------------
	public static class GetSession implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext ctx) {
			return Stream.of(
					Arguments.of(
							"Existing session is returned with 200 OK",
							"get-session/existing-200/engine-create-game.json",
							"get-session/existing-200/expected-session-created.json",
							HttpStatus.OK));
		}
	}

	// ------------------------------------------------------------------
	// POST /sessions/{id}/simulate
	// ------------------------------------------------------------------
	public static class SimulateSession implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext ctx) {
			return Stream.of(
					Arguments.of(
							"Strategy plays both sides -> 9-move DRAW",
							"simulate-session/nine-move-draw/engine-create-game.json",
							List.of(
									"simulate-session/nine-move-draw/move-1-x-center.json",
									"simulate-session/nine-move-draw/move-2-o-tl.json",
									"simulate-session/nine-move-draw/move-3-x-tr.json",
									"simulate-session/nine-move-draw/move-4-o-bl.json",
									"simulate-session/nine-move-draw/move-5-x-left.json",
									"simulate-session/nine-move-draw/move-6-o-right.json",
									"simulate-session/nine-move-draw/move-7-x-br.json",
									"simulate-session/nine-move-draw/move-8-o-top.json",
									"simulate-session/nine-move-draw/move-9-x-draw.json"),
							"simulate-session/nine-move-draw/expected-session-completed-draw.json",
							HttpStatus.OK));
		}
	}

	// ------------------------------------------------------------------
	// Error scenarios on POST /sessions/{id}/simulate
	// ------------------------------------------------------------------
	public static class ErrorScenarios implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext ctx) {
			return Stream.of(
					Arguments.of(
							"Engine 500 -> session 502 BAD_GATEWAY",
							500,
							"{\"message\":\"Engine crashed\"}",
							HttpStatus.BAD_GATEWAY),
					Arguments.of(
							"Engine 404 -> session 502 BAD_GATEWAY",
							404,
							"{\"message\":\"Game not found\"}",
							HttpStatus.BAD_GATEWAY),
					Arguments.of(
							"Engine 400 -> session 502 BAD_GATEWAY",
							400,
							"{\"message\":\"Cell already occupied\"}",
							HttpStatus.BAD_GATEWAY));
		}
	}
}
