package com.example.tictac.engine.model;

import com.example.tictac.common.GameConstants;
import com.example.tictac.common.enums.GameStatus;
import com.example.tictac.common.enums.Player;
import java.util.concurrent.locks.ReentrantLock;

public class Game {

	private final String gameId;
	private final Player[] board = new Player[GameConstants.BOARD_SIZE];
	private GameStatus status;
	private Player winner;
	private Player nextPlayer;
	private final ReentrantLock lock;

	public Game(String gameId) {
		this.gameId = gameId;
		this.status = GameStatus.NEW;
		this.lock = new ReentrantLock();
		nextPlayer = Player.X;
	}

	public String getGameId() {
		return gameId;
	}

	public Player[] getBoard() {
		return board;
	}

	public GameStatus getStatus() {
		return status;
	}

	public void setStatus(GameStatus status) {
		this.status = status;
	}

	public Player getWinner() {
		return winner;
	}

	public void setWinner(Player winner) {
		this.winner = winner;
	}

	public Player getNextPlayer() {
		return nextPlayer;
	}

	public void setNextPlayer(Player nextPlayer) {
		this.nextPlayer = nextPlayer;
	}

	public ReentrantLock lock() {
		return lock;
	}

	public boolean isTerminal() {
		return status != null && status.isTerminal();
	}
}
