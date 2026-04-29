package com.example.tictac.engine.dto;

import com.example.tictac.engine.model.enums.Player;
import com.example.tictac.engine.validation.PositionConstraint;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(
    @NotNull(message = "Player is required (X or O)")
    Player player,

    @PositionConstraint
    @NotNull(message = "Position is required")
    Integer position
) {}
