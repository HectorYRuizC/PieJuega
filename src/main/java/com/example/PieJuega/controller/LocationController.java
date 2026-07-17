package com.example.PieJuega.controller;

import com.example.PieJuega.dto.response.CityResponseDTO;
import com.example.PieJuega.service.CityCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final CityCatalogService cityCatalogService;

    @GetMapping("/cities")
    public List<CityResponseDTO> cities(@RequestParam(defaultValue = "") String query) {
        return cityCatalogService.getCities(query);
    }
}
