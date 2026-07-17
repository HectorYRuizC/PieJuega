package com.example.PieJuega.controller;

import com.example.PieJuega.dto.response.AvailabilitySlotResponseDTO;
import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.service.BookingService;
import com.example.PieJuega.util.TeamFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
public class FieldController {

    private final BookingService bookingService;

    @GetMapping
    public List<FieldResponseDTO> fields(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) TeamFormat format,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String cityCode,
            @RequestParam(required = false) String city
    ) {
        return bookingService.getFields(query, format, latitude, longitude, cityCode, city);
    }

    @GetMapping("/{fieldId}")
    public FieldResponseDTO field(
            @PathVariable Long fieldId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return bookingService.getField(fieldId, latitude, longitude);
    }

    @GetMapping("/{fieldId}/availability")
    public List<AvailabilitySlotResponseDTO> availability(
            @PathVariable Long fieldId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return bookingService.getAvailability(fieldId, date);
    }
}
