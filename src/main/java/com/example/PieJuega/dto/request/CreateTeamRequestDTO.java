package com.example.PieJuega.dto.request;

import com.example.PieJuega.util.TeamFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTeamRequestDTO(
        @NotBlank(message = "El nombre del equipo es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String name,
        @Size(max = 240, message = "La descripción no puede superar 240 caracteres")
        String description,
        @Size(max = 80, message = "La ciudad no puede superar 80 caracteres")
        String city,
        @Size(max = 1000, message = "La URL del escudo es demasiado larga")
        String shieldUrl,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color principal no es válido")
        String primaryColor,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color secundario no es válido")
        String secondaryColor,
        @NotNull(message = "El formato del equipo es obligatorio") TeamFormat format,
        @NotBlank(message = "La formación es obligatoria") String formation,
        @Valid @NotNull(message = "La plantilla es obligatoria") List<TeamMemberRequestDTO> members
) {
}
