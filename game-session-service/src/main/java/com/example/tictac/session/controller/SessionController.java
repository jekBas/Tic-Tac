package com.example.tictac.session.controller;

import com.example.tictac.session.dto.HumanMoveRequest;
import com.example.tictac.session.dto.PlayerVsComputerRequest;
import com.example.tictac.session.dto.SessionResponse;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
@Tag(name = "Sessions", description = "Game session orchestration and gameplay")
public class SessionController {

	private final SessionService sessionService;

	public SessionController(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@PostMapping
	@Operation(summary = "Create simulation session", description = "Creates a new session for automated AI-vs-AI simulation.")
	public ResponseEntity<SessionResponse> createSession() {
		GameSession session = sessionService.createSession();
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(SessionResponse.from(session));
	}

	@PostMapping("/{sessionId}/simulate")
	@Operation(summary = "Run simulation", description = "Starts automated gameplay. Publishes real-time updates via WebSocket.")
	public ResponseEntity<SessionResponse> simulate(@PathVariable String sessionId) {
		GameSession session = sessionService.simulate(sessionId);
		return ResponseEntity.ok(SessionResponse.from(session));
	}

	@GetMapping("/{sessionId}")
	@Operation(summary = "Get session state", description = "Returns session status, current game state, and move history.")
	public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
		GameSession session = sessionService.getSession(sessionId);
		return ResponseEntity.ok(SessionResponse.from(session));
	}

	@PostMapping("/player-vs-computer")
	@Operation(summary = "Create player-vs-computer session", description = "Creates an interactive session. Human chooses side (X/O) and difficulty (SMART/STUPID).")
	public ResponseEntity<SessionResponse> createPlayerVsComputer(@Valid @RequestBody PlayerVsComputerRequest request) {
		GameSession session = sessionService.createPlayerVsComputerSession(
				request.humanPlayer(), request.difficulty());
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(SessionResponse.from(session));
	}

	@PostMapping("/{sessionId}/human-move")
	@Operation(summary = "Submit human move", description = "Places human's mark on the board. Computer responds automatically if the game isn't over.")
	public ResponseEntity<SessionResponse> humanMove(@PathVariable String sessionId,
																									 @Valid @RequestBody HumanMoveRequest request) {
		GameSession session = sessionService.humanMove(sessionId, request.position());
		return ResponseEntity.ok(SessionResponse.from(session));
	}
}
