package com.example.PieJuega.dto.response;

public record ChatEventResponseDTO(
        String type,
        Long roomId,
        ChatMessageResponseDTO message,
        ChatConversationResponseDTO conversation
) {
}
