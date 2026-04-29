package com.example.tictac.engine.model.enums;

public enum Player {
	X,
	O;

	public Player opposite() {
		return switch (this) {
			case X -> O;
			case O -> X;
		};
	}
}
