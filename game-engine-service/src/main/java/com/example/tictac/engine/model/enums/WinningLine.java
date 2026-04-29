package com.example.tictac.engine.model.enums;

public enum WinningLine {
	ROW_1(0, 1, 2),
	ROW_2(3, 4, 5),
	ROW_3(6, 7, 8),
	COL_1(0, 3, 6),
	COL_2(1, 4, 7),
	COL_3(2, 5, 8),
	DIAG_MAIN(0, 4, 8),
	DIAG_SECOND(2, 4, 6);

	private final int first;
	private final int second;
	private final int third;

	WinningLine(int first, int second, int third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public int first() {
		return first;
	}

	public int second() {
		return second;
	}

	public int third() {
		return third;
	}

	public boolean isClaimedBy(Player[] board, Player player) {
		return board[first] == player
				&& board[second] == player
				&& board[third] == player;
	}
}
