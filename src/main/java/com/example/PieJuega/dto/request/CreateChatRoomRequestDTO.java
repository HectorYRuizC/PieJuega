package com.example.PieJuega.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateChatRoomRequestDTO(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String category,
        @Size(max = 1000) String imageUrl,
        Set<Long> memberIds
) {
}
