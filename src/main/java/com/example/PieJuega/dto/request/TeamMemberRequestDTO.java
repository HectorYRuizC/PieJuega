package com.example.PieJuega.dto.request;

import com.example.PieJuega.util.PlayerPosition;
import com.example.PieJuega.util.SquadRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TeamMemberRequestDTO(
        @NotNull(message = "El jugador es obligatorio") Long userId,
        @NotNull(message = "El rol de plantilla es obligatorio") SquadRole squadRole,
        @NotNull(message = "La posición es obligatoria") PlayerPosition position,
        @Min(value = 0, message = "La posición en la plantilla no es válida") int slotIndex,
        boolean captain
) {
}
