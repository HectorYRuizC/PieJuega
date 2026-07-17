package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.UpsertFieldRequestDTO;
import com.example.PieJuega.dto.response.CityResponseDTO;
import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.mapper.FieldMapper;
import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.repository.ReservationRepository;
import com.example.PieJuega.repository.TournamentRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.ReservationStatus;
import com.example.PieJuega.util.TournamentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminFieldService {

    private static final EnumSet<ReservationStatus> ACTIVE_RESERVATION_STATUSES =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);
    private static final EnumSet<TournamentStatus> ACTIVE_TOURNAMENT_STATUSES =
            EnumSet.of(
                    TournamentStatus.PENDING_APPROVAL,
                    TournamentStatus.OPEN_REGISTRATION,
                    TournamentStatus.IN_PROGRESS
            );

    private final FootballFieldRepository fieldRepository;
    private final ReservationRepository reservationRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final CityCatalogService cityCatalogService;
    private final FieldMapper fieldMapper;

    @Transactional(readOnly = true)
    public List<FieldResponseDTO> getFields(Long adminId, String query, Boolean active) {
        requireAdmin(adminId);
        String normalizedQuery = query == null ? "" : query.trim();
        return fieldRepository.searchForAdmin(normalizedQuery, active).stream()
                .map(field -> fieldMapper.toResponse(field, null, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public FieldResponseDTO getField(Long adminId, Long fieldId) {
        requireAdmin(adminId);
        return fieldMapper.toResponse(getFieldEntity(fieldId), null, null);
    }

    @Transactional
    public FieldResponseDTO createField(Long adminId, UpsertFieldRequestDTO request) {
        requireAdmin(adminId);
        CityResponseDTO city = resolveCity(request.cityCode());
        validateSchedule(request);

        FootballField field = FootballField.builder()
                .rating(BigDecimal.ZERO.setScale(1))
                .build();
        apply(field, request, city);
        return fieldMapper.toResponse(fieldRepository.save(field), null, null);
    }

    @Transactional
    public FieldResponseDTO updateField(
            Long adminId,
            Long fieldId,
            UpsertFieldRequestDTO request
    ) {
        requireAdmin(adminId);
        FootballField field = getFieldEntity(fieldId);
        CityResponseDTO city = resolveCity(request.cityCode());
        validateSchedule(request);
        validateOperationalChanges(field, request);
        apply(field, request, city);
        return fieldMapper.toResponse(fieldRepository.save(field), null, null);
    }

    @Transactional
    public FieldResponseDTO setActive(Long adminId, Long fieldId, boolean active) {
        requireAdmin(adminId);
        FootballField field = getFieldEntity(fieldId);
        if (field.isActive() == active) {
            return fieldMapper.toResponse(field, null, null);
        }
        if (!active) {
            requireNoFutureActivity(fieldId);
        }
        field.setActive(active);
        return fieldMapper.toResponse(fieldRepository.save(field), null, null);
    }

    private void apply(
            FootballField field,
            UpsertFieldRequestDTO request,
            CityResponseDTO city
    ) {
        field.setName(request.name().trim());
        field.setAddress(request.address().trim());
        field.setCity(city.name());
        field.setCityCode(city.code());
        field.setLatitude(request.latitude());
        field.setLongitude(request.longitude());
        field.setDescription(request.description().trim());
        field.setImageUrl(blankToNull(request.imageUrl()));
        field.setFormat(request.format());
        field.setPricePerHour(request.pricePerHour());
        field.setOpeningTime(request.openingTime());
        field.setClosingTime(request.closingTime());
        field.setSlotDurationMinutes(request.slotDurationMinutes());
        field.setFeatures(normalizeFeatures(request));
        field.setOpenDays(new LinkedHashSet<>(request.openDays()));
        field.setActive(request.active());
    }

    private LinkedHashSet<String> normalizeFeatures(UpsertFieldRequestDTO request) {
        LinkedHashSet<String> features = new LinkedHashSet<>();
        if (request.features() == null) return features;
        request.features().stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(features::add);
        return features;
    }

    private void validateSchedule(UpsertFieldRequestDTO request) {
        if (!request.closingTime().isAfter(request.openingTime())) {
            throw new IllegalArgumentException("La hora de cierre debe ser posterior a la apertura");
        }
        long minutes = Duration.between(request.openingTime(), request.closingTime()).toMinutes();
        if (minutes % request.slotDurationMinutes() != 0) {
            throw new IllegalArgumentException(
                    "El horario debe dividirse exactamente en turnos de "
                            + request.slotDurationMinutes() + " minutos"
            );
        }
    }

    private void validateOperationalChanges(
            FootballField field,
            UpsertFieldRequestDTO request
    ) {
        boolean scheduleChanged = !field.getOpeningTime().equals(request.openingTime())
                || !field.getClosingTime().equals(request.closingTime())
                || field.getSlotDurationMinutes() != request.slotDurationMinutes()
                || !field.getOpenDays().equals(request.openDays());
        if (scheduleChanged && hasFutureReservations(field.getId())) {
            throw new IllegalArgumentException(
                    "Gestiona primero las reservas futuras antes de cambiar el horario"
            );
        }
        if (field.getFormat() != request.format() && hasActiveTournaments(field.getId())) {
            throw new IllegalArgumentException(
                    "Gestiona primero los torneos activos antes de cambiar el formato"
            );
        }
        if (field.isActive() && !request.active()) {
            requireNoFutureActivity(field.getId());
        }
    }

    private void requireNoFutureActivity(Long fieldId) {
        if (hasFutureReservations(fieldId)) {
            throw new IllegalArgumentException(
                    "No puedes archivar una cancha con reservas futuras pendientes o aprobadas"
            );
        }
        if (hasActiveTournaments(fieldId)) {
            throw new IllegalArgumentException(
                    "No puedes archivar una cancha vinculada a torneos activos"
            );
        }
    }

    private boolean hasFutureReservations(Long fieldId) {
        return reservationRepository.existsByField_IdAndStatusInAndStartAtAfter(
                fieldId,
                ACTIVE_RESERVATION_STATUSES,
                LocalDateTime.now()
        );
    }

    private boolean hasActiveTournaments(Long fieldId) {
        return tournamentRepository.existsByField_IdAndStatusIn(
                fieldId,
                ACTIVE_TOURNAMENT_STATUSES
        );
    }

    private CityResponseDTO resolveCity(String cityCode) {
        return cityCatalogService.resolve(cityCode, null, null)
                .orElseThrow(() -> new IllegalArgumentException("La ciudad seleccionada no es válida"));
    }

    private FootballField getFieldEntity(Long fieldId) {
        return fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
    }

    private void requireAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        boolean admin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        if (!admin) {
            throw new AccessDeniedException("Se requieren permisos de administrador");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
