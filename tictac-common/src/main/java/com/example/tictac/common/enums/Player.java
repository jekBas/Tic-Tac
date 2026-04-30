package com.example.tictac.common.enums;

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
