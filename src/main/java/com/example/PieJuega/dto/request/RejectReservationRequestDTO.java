package com.example.PieJuega.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectReservationRequestDTO(
        @NotBlank(message = "El motivo de rechazo es obligatorio")
        @Size(max = 300, message = "El motivo no puede superar 300 caracteres") String reason
) {
}
