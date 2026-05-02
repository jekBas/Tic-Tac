package com.example.tictac.session.service;

import com.example.tictac.common.GameConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class RandomMoveStrategy {

	private final Random random;

	public RandomMoveStrategy() {
		this(new Random());
	}

	RandomMoveStrategy(Random random) {
		this.random = random;
	}

	public int chooseMove(List<String> board) {
		if (board == null || board.size() != GameConstants.BOARD_SIZE) {
			throw new IllegalStateException("Game Engine returned an invalid board");
		}

		List<Integer> emptyCells = new ArrayList<>();
		for (int i = 0; i < GameConstants.BOARD_SIZE; i++) {
			if (board.get(i) == null) {
				emptyCells.add(i);
			}
		}

		if (emptyCells.isEmpty()) {
			throw new IllegalStateException("No empty cell available on the board");
		}

		return emptyCells.get(random.nextInt(emptyCells.size()));
	}
}