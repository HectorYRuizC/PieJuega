package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.PlayerPosition;
import com.example.PieJuega.util.SquadRole;

public record TeamMemberResponseDTO(
        Long userId,
        String username,
        String photoUrl,
        SquadRole squadRole,
        PlayerPosition position,
        int slotIndex,
        boolean captain
) {
}
