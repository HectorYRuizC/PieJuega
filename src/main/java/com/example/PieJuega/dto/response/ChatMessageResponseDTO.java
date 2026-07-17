package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponseDTO(
        Long id,
        Long roomId,
        Long senderId,
        String senderName,
        String senderPhotoUrl,
        String content,
        ChatMessageType messageType,
        String mediaUrl,
        LocalDateTime timestamp,
        boolean read
) {
}
