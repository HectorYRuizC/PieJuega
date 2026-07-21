package com.example.PieJuega.service;

import com.example.PieJuega.mapper.FieldMapper;
import com.example.PieJuega.model.FieldFavorite;
import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FieldFavoriteRepository;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.TeamFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldFavoriteServiceTests {

    @Mock
    private FieldFavoriteRepository favoriteRepository;

    @Mock
    private FootballFieldRepository fieldRepository;

    @Mock
    private UserRepository userRepository;

    private FieldFavoriteService service;
    private User user;
    private FootballField field;

    @BeforeEach
    void setUp() {
        service = new FieldFavoriteService(
                favoriteRepository,
                fieldRepository,
                userRepository,
                new FieldMapper()
        );
        user = User.builder().id(7L).build();
        field = FootballField.builder()
                .id(4L)
                .name("Cancha Norte")
                .address("Carrera 10 # 20-30")
                .city("Barranquilla")
                .cityCode("08001")
                .description("Cancha sintética")
                .format(TeamFormat.SEVEN)
                .rating(new BigDecimal("4.8"))
                .pricePerHour(new BigDecimal("120000"))
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(22, 0))
                .slotDurationMinutes(60)
                .features(new LinkedHashSet<>(List.of("Parqueadero")))
                .openDays(new LinkedHashSet<>(List.of(DayOfWeek.MONDAY)))
                .active(true)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    void addsAnActiveFieldToFavorites() {
        when(favoriteRepository.findByUser_IdAndField_Id(7L, 4L))
                .thenReturn(Optional.empty());
        when(fieldRepository.findById(4L)).thenReturn(Optional.of(field));

        var response = service.addFavorite(7L, 4L, null, null);

        assertEquals(4L, response.id());
        verify(favoriteRepository).save(any(FieldFavorite.class));
    }

    @Test
    void addingAnExistingFavoriteIsIdempotent() {
        when(favoriteRepository.findByUser_IdAndField_Id(7L, 4L))
                .thenReturn(Optional.of(FieldFavorite.builder()
                        .user(user)
                        .field(field)
                        .build()));

        var response = service.addFavorite(7L, 4L, null, null);

        assertEquals(4L, response.id());
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void removesAnExistingFavorite() {
        FieldFavorite favorite = FieldFavorite.builder()
                .id(12L)
                .user(user)
                .field(field)
                .build();
        when(favoriteRepository.findByUser_IdAndField_Id(7L, 4L))
                .thenReturn(Optional.of(favorite));

        service.removeFavorite(7L, 4L);

        verify(favoriteRepository).delete(favorite);
    }
}
