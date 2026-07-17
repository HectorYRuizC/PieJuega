package com.example.PieJuega.dto.response;

public record PlayerSearchResponseDTO(
        Long id,
        String username,
        String photoUrl,
        String city,
        Double distanceKm
) {
}
