package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.UpsertFieldRequestDTO;
import com.example.PieJuega.dto.response.CityResponseDTO;
import com.example.PieJuega.mapper.FieldMapper;
import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.repository.ReservationRepository;
import com.example.PieJuega.repository.TournamentRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.TeamFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFieldServiceTests {

    @Mock
    private FootballFieldRepository fieldRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CityCatalogService cityCatalogService;

    private AdminFieldService service;

    @BeforeEach
    void setUp() {
        service = new AdminFieldService(
                fieldRepository,
                reservationRepository,
                tournamentRepository,
                userRepository,
                cityCatalogService,
                new FieldMapper()
        );
        User admin = User.builder()
                .id(1L)
                .roles(Set.of(Role.builder().name("ROLE_ADMIN").build()))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    }

    @Test
    void createsAFieldUsingTheCanonicalCity() {
        when(cityCatalogService.resolve("08001", null, null))
                .thenReturn(Optional.of(new CityResponseDTO(
                        "08001",
                        "Barranquilla",
                        "Atlántico"
                )));
        when(fieldRepository.save(any(FootballField.class))).thenAnswer(invocation -> {
            FootballField field = invocation.getArgument(0);
            field.setId(8L);
            return field;
        });

        var response = service.createField(1L, request(true, 60));

        assertEquals(8L, response.id());
        assertEquals("Barranquilla", response.city());
        assertEquals("08001", response.cityCode());
        assertEquals(Set.of("Parqueadero", "Camerinos"), response.features());
        assertEquals(true, response.active());
    }

    @Test
    void rejectsAScheduleThatCannotBeDividedIntoSlots() {
        when(cityCatalogService.resolve("08001", null, null))
                .thenReturn(Optional.of(new CityResponseDTO(
                        "08001",
                        "Barranquilla",
                        "Atlántico"
                )));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createField(1L, request(true, 90))
        );
        verify(fieldRepository, never()).save(any());
    }

    @Test
    void preventsArchivingAFieldWithFutureReservations() {
        FootballField field = FootballField.builder()
                .id(8L)
                .active(true)
                .build();
        when(fieldRepository.findById(8L)).thenReturn(Optional.of(field));
        when(reservationRepository.existsByField_IdAndStatusInAndStartAtAfter(
                eq(8L),
                anyCollection(),
                any(LocalDateTime.class)
        )).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.setActive(1L, 8L, false)
        );
        verify(fieldRepository, never()).save(any());
    }

    private UpsertFieldRequestDTO request(boolean active, int slotMinutes) {
        return new UpsertFieldRequestDTO(
                "Cancha Norte",
                "Carrera 10 # 20-30",
                "08001",
                10.9878,
                -74.7889,
                "Cancha sintética con iluminación",
                null,
                TeamFormat.SEVEN,
                new BigDecimal("120000"),
                LocalTime.of(6, 0),
                LocalTime.of(22, 0),
                slotMinutes,
                new LinkedHashSet<>(Set.of("Parqueadero", "Camerinos")),
                Set.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY
                ),
                active
        );
    }
}
