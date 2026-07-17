package com.example.PieJuega.dto.request;

import com.example.PieJuega.util.TeamFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record UpsertFieldRequestDTO(
        @NotBlank(message = "El nombre de la cancha es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String name,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 160, message = "La dirección admite máximo 160 caracteres")
        String address,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(min = 5, max = 5, message = "El código de ciudad no es válido")
        String cityCode,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud no es válida")
        @DecimalMax(value = "90", message = "La latitud no es válida")
        Double latitude,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud no es válida")
        @DecimalMax(value = "180", message = "La longitud no es válida")
        Double longitude,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 600, message = "La descripción admite máximo 600 caracteres")
        String description,

        @Size(max = 1000, message = "La URL de imagen es demasiado larga")
        String imageUrl,

        @NotNull(message = "El formato de juego es obligatorio")
        TeamFormat format,

        @NotNull(message = "El precio por hora es obligatorio")
        @DecimalMin(value = "1000", message = "El precio por hora debe ser mayor a $1.000")
        BigDecimal pricePerHour,

        @NotNull(message = "La hora de apertura es obligatoria")
        LocalTime openingTime,

        @NotNull(message = "La hora de cierre es obligatoria")
        LocalTime closingTime,

        @Min(value = 30, message = "Los turnos deben durar al menos 30 minutos")
        @Max(value = 180, message = "Los turnos admiten máximo 180 minutos")
        int slotDurationMinutes,

        @Size(max = 12, message = "Puedes registrar máximo 12 servicios")
        Set<@NotBlank(message = "Los servicios no pueden estar vacíos")
                @Size(max = 80, message = "Cada servicio admite máximo 80 caracteres") String> features,

        @NotEmpty(message = "Selecciona al menos un día de atención")
        Set<DayOfWeek> openDays,

        boolean active
) {
}
