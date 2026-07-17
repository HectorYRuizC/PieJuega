package com.example.PieJuega.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CityCatalogServiceTests {

    private final CityCatalogService service = new CityCatalogService(new ObjectMapper());

    @Test
    void loadsTheCompleteDivipolaMunicipalityCatalog() {
        assertThat(service.getCities("")).hasSize(1122);
    }

    @Test
    void resolvesCitiesByCodeAndAccentInsensitiveName() {
        assertThat(service.resolve("08001", null, null))
                .get()
                .extracting(city -> city.name())
                .isEqualTo("Barranquilla");
        assertThat(service.resolve(null, "Bogotá", "Bogotá D.C."))
                .get()
                .extracting(city -> city.code())
                .isEqualTo("11001");
        assertThat(service.resolve(null, "Medellín", "Antioquia"))
                .get()
                .extracting(city -> city.code())
                .isEqualTo("05001");
    }
}
