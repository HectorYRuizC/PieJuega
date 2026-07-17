package com.example.PieJuega.dto.response;

import java.util.List;

public record ChatRoomDetailsResponseDTO(
        Long roomId,
        String name,
        String category,
        String imageUrl,
        List<ChatMemberResponseDTO> members
) {
}
