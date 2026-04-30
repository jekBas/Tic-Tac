package com.example.tictac.session.exception;

public class SimulationAlreadyRunningException extends RuntimeException {

	public SimulationAlreadyRunningException(String sessionId) {
		super("Simulation is already running for session " + sessionId);
	}
}
