package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.CityResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class CityCatalogService {

    private static final Map<String, String> DISPLAY_NAME_OVERRIDES = Map.of(
            "05001", "Medellín",
            "11001", "Bogotá D.C."
    );

    private final List<CityResponseDTO> cities;

    public CityCatalogService(ObjectMapper objectMapper) {
        try (var input = new ClassPathResource("data/divipola_cities.json").getInputStream()) {
            List<CityResponseDTO> source = objectMapper.readValue(
                    input,
                    new TypeReference<List<CityResponseDTO>>() {
                    }
            );
            cities = source.stream()
                    .map(city -> new CityResponseDTO(
                            city.code(),
                            DISPLAY_NAME_OVERRIDES.getOrDefault(city.code(), titleCase(city.name())),
                            city.department()
                    ))
                    .sorted(Comparator
                            .comparing(CityResponseDTO::department)
                            .thenComparing(CityResponseDTO::name))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo cargar el catálogo DIVIPOLA", exception);
        }
    }

    public List<CityResponseDTO> getCities(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return cities;
        }
        return cities.stream()
                .filter(city -> normalize(city.name()).contains(normalizedQuery)
                        || normalize(city.department()).contains(normalizedQuery)
                        || city.code().contains(normalizedQuery))
                .toList();
    }

    public Optional<CityResponseDTO> resolve(String code, String cityName, String department) {
        if (code != null && !code.isBlank()) {
            return cities.stream().filter(city -> city.code().equals(code.trim())).findFirst();
        }

        String normalizedName = withoutDistrictSuffix(cityName);
        String normalizedDepartment = withoutDistrictSuffix(department);
        if (normalizedName.isEmpty()) {
            return Optional.empty();
        }

        List<CityResponseDTO> matches = cities.stream()
                .filter(city -> withoutDistrictSuffix(city.name()).equals(normalizedName))
                .toList();
        if (!normalizedDepartment.isEmpty()) {
            Optional<CityResponseDTO> departmentMatch = matches.stream()
                    .filter(city -> withoutDistrictSuffix(city.department())
                            .equals(normalizedDepartment))
                    .findFirst();
            if (departmentMatch.isPresent()) {
                return departmentMatch;
            }
        }
        return matches.stream().findFirst();
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String withoutDistrictSuffix(String value) {
        return normalize(value).replaceFirst(" D C$", "");
    }

    private static String titleCase(String value) {
        return Arrays.stream(value.toLowerCase(Locale.forLanguageTag("es-CO")).split(" "))
                .map(word -> word.contains(".")
                        ? word.toUpperCase(Locale.ROOT)
                        : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(value);
    }
}
