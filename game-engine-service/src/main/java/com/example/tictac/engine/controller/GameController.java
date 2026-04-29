package com.example.tictac.engine.controller;

import com.example.tictac.engine.dto.GameResponse;
import com.example.tictac.engine.dto.MoveRequest;
import com.example.tictac.engine.model.Game;
import com.example.tictac.engine.service.GameService;
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
public class GameController {

	private final GameService gameService;

	public GameController(GameService gameService) {
		this.gameService = gameService;
	}

	@PostMapping
	public ResponseEntity<GameResponse> createGame() {
		Game game = gameService.createGame();
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(GameResponse.from(game));
	}

	@GetMapping("/{gameId}")
	public ResponseEntity<GameResponse> getGame(@PathVariable String gameId) {
		Game game = gameService.getGame(gameId);
		return ResponseEntity.ok(GameResponse.from(game));
	}

	@PostMapping("/{gameId}/move")
	public ResponseEntity<GameResponse> applyMove(@PathVariable String gameId,
																								@Valid @RequestBody MoveRequest moveRequest) {
		Game game = gameService.applyMove(gameId, moveRequest);
		return ResponseEntity.ok(GameResponse.from(game));
	}
}
