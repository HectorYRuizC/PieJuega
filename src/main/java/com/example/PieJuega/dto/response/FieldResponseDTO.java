package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.TeamFormat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record FieldResponseDTO(
        Long id,
        String name,
        String address,
        String city,
        Double latitude,
        Double longitude,
        Double distanceKm,
        String description,
        String imageUrl,
        TeamFormat format,
        BigDecimal rating,
        BigDecimal pricePerHour,
        LocalTime openingTime,
        LocalTime closingTime,
        int slotDurationMinutes,
        Set<String> features,
        Set<DayOfWeek> openDays
) {
}
