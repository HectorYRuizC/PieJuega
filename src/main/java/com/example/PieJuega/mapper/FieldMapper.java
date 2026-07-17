package com.example.PieJuega.mapper;

import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.util.GeoUtils;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FieldMapper {

    public FieldResponseDTO toResponse(
            FootballField field,
            Double latitude,
            Double longitude
    ) {
        return new FieldResponseDTO(
                field.getId(),
                field.getName(),
                field.getAddress(),
                field.getCity(),
                field.getCityCode(),
                field.getLatitude(),
                field.getLongitude(),
                GeoUtils.distanceKm(latitude, longitude, field.getLatitude(), field.getLongitude()),
                field.getDescription(),
                field.getImageUrl(),
                field.getFormat(),
                field.getRating(),
                field.getPricePerHour(),
                field.getOpeningTime(),
                field.getClosingTime(),
                field.getSlotDurationMinutes(),
                Set.copyOf(field.getFeatures()),
                Set.copyOf(field.getOpenDays()),
                field.isActive()
        );
    }
}
