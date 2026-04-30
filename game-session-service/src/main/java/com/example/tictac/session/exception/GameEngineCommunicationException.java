package com.example.tictac.session.exception;

/**
 * Raised when the Game Engine Service is unreachable, returns an unexpected
 * status, or otherwise fails to honour an orchestration request.
 */
public class GameEngineCommunicationException extends RuntimeException {

	public GameEngineCommunicationException(String message) {
		super(message);
	}

	public GameEngineCommunicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
