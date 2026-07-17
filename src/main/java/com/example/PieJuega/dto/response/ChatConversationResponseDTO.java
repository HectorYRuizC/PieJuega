package com.example.PieJuega.dto.response;

import java.time.LocalDateTime;

public record ChatConversationResponseDTO(
        Long roomId,
        String name,
        String category,
        String imageUrl,
        long memberCount,
        String lastMessage,
        String lastMessageSenderName,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
