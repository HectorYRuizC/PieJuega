package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.NotificationType;

import java.time.LocalDateTime;

public record AppNotificationResponseDTO(
        Long id,
        NotificationType type,
        String title,
        String body,
        String destination,
        Long sourceId,
        boolean read,
        LocalDateTime createdAt
) {
}
