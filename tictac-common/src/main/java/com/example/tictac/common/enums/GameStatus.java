package com.example.tictac.common.enums;

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

	public boolean isTerminal() {
		return this == X_WON || this == O_WON || this == DRAW;
	}
}
