package com.example.PieJuega.dto.request;

import com.example.PieJuega.util.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterPushDeviceRequestDTO(
        @NotBlank @Size(max = 512) String token,
        @NotNull DevicePlatform platform,
        Boolean soundEnabled,
        Boolean vibrationEnabled
) {
}
