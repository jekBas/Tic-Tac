package com.example.tictac.session.model.enums;

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
