package com.example.PieJuega.dto.request;

import jakarta.validation.constraints.NotNull;

public record JoinTournamentRequestDTO(@NotNull Long teamId) {
}
