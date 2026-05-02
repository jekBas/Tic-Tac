package com.example.tictac.session.dto;

import com.example.tictac.common.enums.Player;
import com.example.tictac.session.model.enums.ComputerDifficulty;
import jakarta.validation.constraints.NotNull;

public record PlayerVsComputerRequest(
		@NotNull(message = "humanPlayer is required (X or O)")
		Player humanPlayer,
		ComputerDifficulty difficulty
) {
	public PlayerVsComputerRequest {
		if (difficulty == null) {
			difficulty = ComputerDifficulty.SMART;
		}
	}
}