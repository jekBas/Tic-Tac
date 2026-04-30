package com.example.tictac.common.dto;

import com.example.tictac.common.enums.Player;
import com.example.tictac.common.validation.PositionConstraint;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(
    @NotNull(message = "Player is required (X or O)")
    Player player,

    @PositionConstraint
    @NotNull(message = "Position is required")
    Integer position
) {}