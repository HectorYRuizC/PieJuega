package com.example.PieJuega.dto.request;

import com.example.PieJuega.util.PaymentMethod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateReservationRequestDTO(
        @NotNull(message = "La cancha es obligatoria") Long fieldId,
        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La reserva debe ser para una fecha futura") LocalDateTime startAt,
        @NotBlank(message = "El nombre de contacto es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres") String contactName,
        @NotBlank(message = "El teléfono de contacto es obligatorio")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "El teléfono debe contener entre 10 y 15 dígitos") String contactPhone,
        @NotNull(message = "El medio de pago es obligatorio") PaymentMethod paymentMethod,
        @Size(max = 500, message = "La nota no puede superar 500 caracteres") String note
) {
}
