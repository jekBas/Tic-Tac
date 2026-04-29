package com.example.tictac.engine.model.enums;

public enum GameStatus {
	NEW,
	IN_PROGRESS,
	X_WON,
	O_WON,
	DRAW;

	public static GameStatus wonBy(Player player) {
		return switch (player) {
			case X -> X_WON;
			case O -> O_WON;
		};
	}
}
