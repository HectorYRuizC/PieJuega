package com.example.PieJuega.dto.response;

import java.time.LocalDateTime;

public record AvailabilitySlotResponseDTO(
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean available
) {
}
