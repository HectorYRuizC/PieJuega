package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.TeamFormat;

import java.time.LocalDateTime;
import java.util.List;

public record TeamResponseDTO(
        Long id,
        String name,
        String description,
        String city,
        String shieldUrl,
        String primaryColor,
        String secondaryColor,
        TeamFormat format,
        String formation,
        Long ownerId,
        Long chatRoomId,
        int memberCount,
        LocalDateTime createdAt,
        List<TeamMemberResponseDTO> members
) {
}
