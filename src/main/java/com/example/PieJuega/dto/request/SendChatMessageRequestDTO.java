package com.example.PieJuega.dto.request;

import com.example.PieJuega.util.ChatMessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequestDTO(
        @Size(max = 4000) String content,
        @NotNull ChatMessageType messageType,
        @Size(max = 1000) String mediaUrl
) {
}
