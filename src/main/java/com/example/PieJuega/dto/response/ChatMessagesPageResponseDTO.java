package com.example.PieJuega.dto.response;

import java.util.List;

public record ChatMessagesPageResponseDTO(
        List<ChatMessageResponseDTO> messages,
        boolean hasMore,
        Long nextBeforeId
) {
}
