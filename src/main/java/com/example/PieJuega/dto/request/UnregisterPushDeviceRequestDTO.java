package com.example.PieJuega.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnregisterPushDeviceRequestDTO(
        @NotBlank @Size(max = 512) String token
) {
}
