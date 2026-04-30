package com.example.tictac.session.model.enums;

public enum GameStatus {
	NEW,
	IN_PROGRESS,
	X_WON,
	O_WON,
	DRAW;

	public boolean isTerminal() {
		return this == X_WON || this == O_WON || this == DRAW;
	}
}
