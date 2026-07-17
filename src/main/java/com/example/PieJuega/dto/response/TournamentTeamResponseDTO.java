package com.example.PieJuega.dto.response;

import java.time.LocalDateTime;

public record TournamentTeamResponseDTO(
        Long id,
        String name,
        String shieldUrl,
        String city,
        LocalDateTime registeredAt
) {
}
