package com.example.PieJuega.config;

import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.util.TeamFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@Profile("dev")
@Order(10)
@RequiredArgsConstructor
public class DevFieldDataInitializer implements ApplicationRunner {

    private final FootballFieldRepository fieldRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (fieldRepository.count() > 0) {
            List<FootballField> fields = fieldRepository.findAll();
            fields.forEach(this::applyCoordinates);
            fieldRepository.saveAll(fields);
            return;
        }

        fieldRepository.saveAll(List.of(
                field(
                        "El Tiburón",
                        "Calle 30 #15-04",
                        TeamFormat.FIVE,
                        "Cancha sintética cómoda para partidos con amigos, entrenamientos y torneos rápidos.",
                        "5.0",
                        "90000",
                        LocalTime.of(13, 0),
                        LocalTime.of(22, 0),
                        EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY),
                        "Grama sintética", "Iluminación", "Parqueadero", "Camerinos"
                ),
                field(
                        "La Bombonera",
                        "Carrera 46 #72-18",
                        TeamFormat.SEVEN,
                        "Espacio amplio para fútbol 7 con iluminación nocturna y zona social.",
                        "4.8",
                        "140000",
                        LocalTime.of(16, 0),
                        LocalTime.of(23, 0),
                        EnumSet.allOf(DayOfWeek.class),
                        "Grama sintética", "Iluminación", "Zona social", "Duchas"
                ),
                field(
                        "Cancha Norte",
                        "Calle 93 #43-21",
                        TeamFormat.FIVE,
                        "Cancha ágil y bien ubicada para entrenamientos y partidos cortos.",
                        "4.7",
                        "85000",
                        LocalTime.of(15, 0),
                        LocalTime.of(21, 0),
                        EnumSet.range(DayOfWeek.TUESDAY, DayOfWeek.SUNDAY),
                        "Grama sintética", "Iluminación", "Cafetería"
                ),
                field(
                        "La 30 Arena",
                        "Avenida 30 #8-55",
                        TeamFormat.EIGHT,
                        "Arena de gran formato para encuentros competitivos, eventos y torneos.",
                        "4.9",
                        "180000",
                        LocalTime.of(14, 0),
                        LocalTime.of(23, 0),
                        EnumSet.allOf(DayOfWeek.class),
                        "Grama profesional", "Iluminación", "Tribuna", "Parqueadero"
                )
        ));
    }

    private void applyCoordinates(FootballField field) {
        field.setCity("Barranquilla");
        field.setCityCode("08001");
        double[] coordinates = switch (field.getName()) {
            case "El Tiburón" -> new double[]{10.9639, -74.7964};
            case "La Bombonera" -> new double[]{11.0008, -74.8068};
            case "Cancha Norte" -> new double[]{11.0177, -74.8275};
            case "La 30 Arena" -> new double[]{10.9485, -74.7858};
            default -> null;
        };
        if (coordinates != null) {
            field.setLatitude(coordinates[0]);
            field.setLongitude(coordinates[1]);
        }
    }

    private FootballField field(
            String name,
            String address,
            TeamFormat format,
            String description,
            String rating,
            String price,
            LocalTime opening,
            LocalTime closing,
            EnumSet<DayOfWeek> openDays,
            String... features
    ) {
        FootballField field = FootballField.builder()
                .name(name)
                .address(address)
                .city("Barranquilla")
                .cityCode("08001")
                .description(description)
                .format(format)
                .rating(new BigDecimal(rating))
                .pricePerHour(new BigDecimal(price))
                .openingTime(opening)
                .closingTime(closing)
                .slotDurationMinutes(60)
                .active(true)
                .openDays(new LinkedHashSet<>(openDays))
                .features(new LinkedHashSet<>(List.of(features)))
                .build();
        applyCoordinates(field);
        return field;
    }
}
