package com.example.PieJuega.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTournamentRequestDTO(
        @NotBlank @Size(max = 300) String reason
) {
}
