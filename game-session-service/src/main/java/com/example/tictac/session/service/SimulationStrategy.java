package com.example.tictac.session.service;

import com.example.tictac.common.GameConstants;
import com.example.tictac.common.enums.Player;
import com.example.tictac.common.enums.WinningLine;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Picks the next move for the rule-based simulation.
 *
 * <p>The strategy is deliberately simple but better than purely random play:
 * <ol>
 *   <li>Take a winning move if one exists for the current player.</li>
 *   <li>Block the opponent's winning move.</li>
 *   <li>Take the centre cell.</li>
 *   <li>Take a corner.</li>
 *   <li>Take a side.</li>
 *   <li>Otherwise the first available cell.</li>
 * </ol>
 *
 * <p>The Game Engine remains authoritative for validation. This strategy only
 * proposes a candidate position from currently empty cells.
 */
@Component
public class SimulationStrategy {

	private static final int CENTER = 4;
	private static final int[] CORNERS = {0, 2, 6, 8};
	private static final int[] SIDES = {1, 3, 5, 7};

	/**
	 * Choose a position from the empty cells of {@code board} for {@code player}.
	 *
	 * @throws IllegalStateException if the board has no empty cell
	 */
	public int chooseMove(List<String> board,
												Player player) {
		if (board == null || board.size() != GameConstants.BOARD_SIZE) {
			throw new IllegalStateException("Game Engine returned an invalid board");
		}

		String me = player.name();
		String opponent = player.opposite().name();

		int winning = findCompletingMove(board, me);
		if (winning >= 0) {
			return winning;
		}

		int blocking = findCompletingMove(board, opponent);
		if (blocking >= 0) {
			return blocking;
		}

		if (isEmpty(board, CENTER)) {
			return CENTER;
		}

		int corner = firstEmpty(board, CORNERS);
		if (corner >= 0) {
			return corner;
		}

		int side = firstEmpty(board, SIDES);
		if (side >= 0) {
			return side;
		}

		for (int i = 0; i < GameConstants.BOARD_SIZE; i++) {
			if (isEmpty(board, i)) {
				return i;
			}
		}

		throw new IllegalStateException("No empty cell available on the board");
	}

	private static int findCompletingMove(List<String> board,
																				String mark) {
		for (WinningLine line : WinningLine.values()) {
			int emptyIndex = -1;
			int marked = 0;
			for (int idx : new int[] {line.first(), line.second(), line.third()}) {
				String cell = board.get(idx);
				if (mark.equals(cell)) {
					marked++;
				} else if (cell == null) {
					emptyIndex = idx;
				} else {
					emptyIndex = -2;
					break;
				}
			}
			if (marked == 2 && emptyIndex >= 0) {
				return emptyIndex;
			}
		}
		return -1;
	}

	private static int firstEmpty(List<String> board, int[] candidates) {
		for (int idx : candidates) {
			if (isEmpty(board, idx)) {
				return idx;
			}
		}
		return -1;
	}

	private static boolean isEmpty(List<String> board, int index) {
		return board.get(index) == null;
	}
}
