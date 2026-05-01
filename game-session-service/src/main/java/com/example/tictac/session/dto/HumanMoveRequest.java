package com.example.tictac.session.dto;

import com.example.tictac.common.validation.PositionConstraint;
import jakarta.validation.constraints.NotNull;

public record HumanMoveRequest(
		@NotNull(message = "Position is required")
		@PositionConstraint
		Integer position
) {}