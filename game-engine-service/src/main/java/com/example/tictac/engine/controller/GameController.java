package com.example.tictac.engine.controller;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.dto.MoveRequest;
import com.example.tictac.engine.mapping.GameStateDtoMapper;
import com.example.tictac.engine.model.Game;
import com.example.tictac.engine.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/games")
@Tag(name = "Games", description = "Game state management and move execution")
public class GameController {

	private final GameService gameService;

	public GameController(GameService gameService) {
		this.gameService = gameService;
	}

	@PostMapping
	@Operation(summary = "Create a new game", description = "Creates a game with an empty 3x3 board. X moves first.")
	public ResponseEntity<GameStateDto> createGame() {
		Game game = gameService.createGame();
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(GameStateDtoMapper.from(game));
	}

	@GetMapping("/{gameId}")
	@Operation(summary = "Get game state", description = "Returns the current board, status, winner, and next player.")
	public ResponseEntity<GameStateDto> getGame(@PathVariable String gameId) {
		Game game = gameService.getGame(gameId);
		return ResponseEntity.ok(GameStateDtoMapper.from(game));
	}

	@PostMapping("/{gameId}/move")
	@Operation(summary = "Apply a move", description = "Places a mark on the board. Validates turn order, position bounds, and game completion.")
	public ResponseEntity<GameStateDto> applyMove(@PathVariable String gameId,
																								@Valid @RequestBody MoveRequest moveRequest) {
		Game game = gameService.applyMove(gameId, moveRequest);
		return ResponseEntity.ok(GameStateDtoMapper.from(game));
	}
}
