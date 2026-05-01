package com.example.tictac.session.dto;

import com.example.tictac.common.enums.Player;
import jakarta.validation.constraints.NotNull;

public record PlayerVsComputerRequest(
		@NotNull(message = "humanPlayer is required (X or O)")
		Player humanPlayer
) {}