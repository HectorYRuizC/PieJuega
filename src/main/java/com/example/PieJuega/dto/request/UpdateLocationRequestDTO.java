package com.example.PieJuega.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdateLocationRequestDTO(
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 80) String city,
        @Size(max = 80) String department,
        @Size(min = 5, max = 5) String cityCode
) {
}
