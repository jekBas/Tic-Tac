package com.example.tictac.session.exception;

public class GameEngineTransientException extends GameEngineCommunicationException {

	public GameEngineTransientException(String message) {
		super(message);
	}

	public GameEngineTransientException(String message, Throwable cause) {
		super(message, cause);
	}
}