package com.example.tictac.session.controller;

import com.example.tictac.session.dto.SessionResponse;
import com.example.tictac.session.model.GameSession;
import com.example.tictac.session.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
public class SessionController {

	private final SessionService sessionService;

	public SessionController(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@PostMapping
	public ResponseEntity<SessionResponse> createSession() {
		GameSession session = sessionService.createSession();
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(SessionResponse.from(session));
	}

	@PostMapping("/{sessionId}/simulate")
	public ResponseEntity<SessionResponse> simulate(@PathVariable String sessionId) {
		GameSession session = sessionService.simulate(sessionId);
		return ResponseEntity.ok(SessionResponse.from(session));
	}

	@GetMapping("/{sessionId}")
	public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
		GameSession session = sessionService.getSession(sessionId);
		return ResponseEntity.ok(SessionResponse.from(session));
	}
}
