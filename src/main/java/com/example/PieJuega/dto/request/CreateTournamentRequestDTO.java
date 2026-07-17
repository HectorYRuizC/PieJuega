package com.example.PieJuega.dto.request;

import com.example.PieJuega.util.TeamFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateTournamentRequestDTO(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 800) String description,
        @Size(max = 1200) String rules,
        @NotNull TeamFormat format,
        @NotNull Long fieldId,
        @NotNull @Future LocalDateTime startsAt,
        @NotNull @Future LocalDateTime registrationDeadline,
        @Min(2) @Max(32) int maxTeams,
        @NotNull @DecimalMin("0.0") BigDecimal entryFee,
        @Size(max = 200) String prize
) {
}
