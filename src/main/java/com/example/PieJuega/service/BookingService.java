package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.CreateReservationRequestDTO;
import com.example.PieJuega.dto.response.AvailabilitySlotResponseDTO;
import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.dto.response.ReservationResponseDTO;
import com.example.PieJuega.exception.ReservationConflictException;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.model.Reservation;
import com.example.PieJuega.model.User;
import com.example.PieJuega.mapper.FieldMapper;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.repository.ReservationRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.ReservationStatus;
import com.example.PieJuega.util.TeamFormat;
import com.example.PieJuega.util.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final EnumSet<ReservationStatus> BLOCKING_STATUSES =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);
    private static final int MAX_ADVANCE_DAYS = 60;

    private final FootballFieldRepository fieldRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CityCatalogService cityCatalogService;
    private final AppNotificationService notificationService;
    private final FieldMapper fieldMapper;

    @Transactional(readOnly = true)
    public List<FieldResponseDTO> getFields(
            String query,
            TeamFormat format,
            Double latitude,
            Double longitude,
            String cityCode,
            String city
    ) {
        String normalizedQuery = query == null ? "" : query.trim();
        var selectedCity = cityCatalogService.resolve(cityCode, city, null);
        if (selectedCity.isEmpty()) {
            return List.of();
        }
        return fieldRepository.searchActive(
                        normalizedQuery,
                        format,
                        selectedCity.get().code(),
                        selectedCity.get().name()
                ).stream()
                .map(field -> fieldMapper.toResponse(field, latitude, longitude))
                .sorted(Comparator
                        .comparing((FieldResponseDTO field) -> field.distanceKm() == null
                                ? Double.MAX_VALUE
                                : field.distanceKm())
                        .thenComparing(FieldResponseDTO::rating, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FieldResponseDTO getField(Long fieldId, Double latitude, Double longitude) {
        FootballField field = fieldRepository.findById(fieldId)
                .filter(FootballField::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        return fieldMapper.toResponse(field, latitude, longitude);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponseDTO> getAvailability(Long fieldId, LocalDate date) {
        FootballField field = fieldRepository.findById(fieldId)
                .filter(FootballField::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        validateAvailabilityDate(date);

        if (!field.getOpenDays().contains(date.getDayOfWeek())) {
            return List.of();
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Reservation> reservations = reservationRepository
                .findByField_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                        fieldId,
                        BLOCKING_STATUSES,
                        dayEnd,
                        dayStart
                );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cursor = date.atTime(field.getOpeningTime());
        LocalDateTime closing = date.atTime(field.getClosingTime());
        Duration slotDuration = Duration.ofMinutes(field.getSlotDurationMinutes());
        java.util.ArrayList<AvailabilitySlotResponseDTO> slots = new java.util.ArrayList<>();

        while (!cursor.plus(slotDuration).isAfter(closing)) {
            LocalDateTime slotStart = cursor;
            LocalDateTime slotEnd = cursor.plus(slotDuration);
            boolean overlaps = reservations.stream().anyMatch(reservation ->
                    reservation.getStartAt().isBefore(slotEnd)
                            && reservation.getEndAt().isAfter(slotStart));
            slots.add(new AvailabilitySlotResponseDTO(
                    slotStart,
                    slotEnd,
                    slotStart.isAfter(now) && !overlaps
            ));
            cursor = slotEnd;
        }
        return List.copyOf(slots);
    }

    @Transactional
    public ReservationResponseDTO createReservation(
            Long userId,
            CreateReservationRequestDTO request
    ) {
        User user = getUser(userId);
        FootballField field = fieldRepository.findActiveByIdForUpdate(request.fieldId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));

        LocalDateTime startAt = request.startAt().withSecond(0).withNano(0);
        LocalDateTime endAt = startAt.plusMinutes(field.getSlotDurationMinutes());
        validateReservationTime(field, startAt, endAt);

        boolean occupied = reservationRepository
                .existsByField_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                        field.getId(),
                        BLOCKING_STATUSES,
                        endAt,
                        startAt
                );
        if (occupied) {
            throw new ReservationConflictException("Este horario acaba de ser reservado");
        }

        BigDecimal totalPrice = calculateTotalPrice(field, field.getSlotDurationMinutes());
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .field(field)
                .user(user)
                .startAt(startAt)
                .endAt(endAt)
                .contactName(request.contactName().trim())
                .contactPhone(request.contactPhone().trim())
                .totalPrice(totalPrice)
                .paymentMethod(request.paymentMethod())
                .note(blankToNull(request.note()))
                .status(ReservationStatus.PENDING)
                .build());
        notificationService.notifyAdministrators(
                NotificationType.RESERVATION_REQUEST,
                "Nueva solicitud de reserva",
                user.getUsername() + " solicitó " + field.getName(),
                "/reservationsAdmin/" + reservation.getId(),
                reservation.getId()
        );
        return toReservationResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getMyReservations(Long userId) {
        getUser(userId);
        return reservationRepository.findByUser_IdOrderByStartAtDesc(userId).stream()
                .map(this::toReservationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponseDTO getReservation(Long reservationId, Long requesterId) {
        User requester = getUser(requesterId);
        Reservation reservation = getReservationEntity(reservationId);
        if (!reservation.getUser().getId().equals(requesterId) && !isAdmin(requester)) {
            throw new AccessDeniedException("No tienes acceso a esta reserva");
        }
        return toReservationResponse(reservation);
    }

    @Transactional
    public ReservationResponseDTO cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = getReservationEntity(reservationId);
        if (!reservation.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("No puedes cancelar esta reserva");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new IllegalArgumentException("Esta reserva ya no se puede cancelar");
        }
        if (!reservation.getStartAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se puede cancelar una reserva que ya inició");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRejectionReason(null);
        Reservation saved = reservationRepository.save(reservation);
        notificationService.notifyAdministrators(
                NotificationType.RESERVATION_CANCELLED,
                "Reserva cancelada",
                reservation.getUser().getUsername() + " canceló "
                        + reservation.getField().getName(),
                "/reservationsAdmin/" + reservation.getId(),
                reservation.getId()
        );
        return toReservationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getAdminReservations(
            Long adminId,
            ReservationStatus status
    ) {
        requireAdmin(adminId);
        List<Reservation> reservations = status == null
                ? reservationRepository.findAllByOrderByStartAtAsc()
                : reservationRepository.findByStatusOrderByStartAtAsc(status);
        return reservations.stream().map(this::toReservationResponse).toList();
    }

    @Transactional
    public ReservationResponseDTO approveReservation(Long reservationId, Long adminId) {
        requireAdmin(adminId);
        Reservation reservation = getReservationEntity(reservationId);
        requirePending(reservation);
        if (!reservation.getStartAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se puede aprobar una reserva vencida");
        }
        reservation.setStatus(ReservationStatus.APPROVED);
        reservation.setRejectionReason(null);
        Reservation saved = reservationRepository.save(reservation);
        notificationService.notifyUser(
                reservation.getUser(),
                NotificationType.RESERVATION_APPROVED,
                "Reserva aprobada",
                "Tu reserva en " + reservation.getField().getName() + " fue confirmada",
                "/reservations/" + reservation.getId(),
                reservation.getId()
        );
        return toReservationResponse(saved);
    }

    @Transactional
    public ReservationResponseDTO rejectReservation(
            Long reservationId,
            Long adminId,
            String reason
    ) {
        requireAdmin(adminId);
        Reservation reservation = getReservationEntity(reservationId);
        requirePending(reservation);
        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setRejectionReason(reason.trim());
        Reservation saved = reservationRepository.save(reservation);
        notificationService.notifyUser(
                reservation.getUser(),
                NotificationType.RESERVATION_REJECTED,
                "Reserva no aprobada",
                "Revisa la decisión sobre tu reserva en "
                        + reservation.getField().getName(),
                "/reservations/" + reservation.getId(),
                reservation.getId()
        );
        return toReservationResponse(saved);
    }

    private void validateAvailabilityDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new IllegalArgumentException("La fecha debe estar dentro de los próximos 60 días");
        }
    }

    private void validateReservationTime(
            FootballField field,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime now = LocalDateTime.now();
        if (!startAt.isAfter(now)) {
            throw new IllegalArgumentException("La reserva debe ser para una fecha futura");
        }
        if (startAt.toLocalDate().isAfter(LocalDate.now().plusDays(MAX_ADVANCE_DAYS))) {
            throw new IllegalArgumentException("Solo puedes reservar con 60 días de anticipación");
        }
        if (!field.getOpenDays().contains(startAt.getDayOfWeek())) {
            throw new IllegalArgumentException("La cancha no abre el día seleccionado");
        }

        LocalDateTime opening = startAt.toLocalDate().atTime(field.getOpeningTime());
        LocalDateTime closing = startAt.toLocalDate().atTime(field.getClosingTime());
        long minutesFromOpening = Duration.between(opening, startAt).toMinutes();
        if (startAt.isBefore(opening)
                || endAt.isAfter(closing)
                || minutesFromOpening % field.getSlotDurationMinutes() != 0) {
            throw new IllegalArgumentException("El horario no corresponde a un turno disponible");
        }
    }

    private void requirePending(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("La reserva ya fue gestionada");
        }
    }

    private User requireAdmin(Long userId) {
        User user = getUser(userId);
        if (!isAdmin(user)) {
            throw new AccessDeniedException("Se requieren permisos de administrador");
        }
        return user;
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Reservation getReservationEntity(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
    }

    private ReservationResponseDTO toReservationResponse(Reservation reservation) {
        FootballField field = reservation.getField();
        User user = reservation.getUser();
        long durationMinutes = Duration.between(
                reservation.getStartAt(),
                reservation.getEndAt()
        ).toMinutes();
        BigDecimal totalPrice = reservation.getTotalPrice() == null
                ? calculateTotalPrice(field, durationMinutes)
                : reservation.getTotalPrice();

        return new ReservationResponseDTO(
                reservation.getId(),
                field.getId(),
                field.getName(),
                field.getFormat(),
                field.getAddress(),
                field.getCity(),
                field.getImageUrl(),
                totalPrice,
                user.getId(),
                user.getUsername(),
                user.getPhotoUrl(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getContactName(),
                reservation.getContactPhone(),
                reservation.getPaymentMethod(),
                reservation.getNote(),
                reservation.getStatus(),
                reservation.getRejectionReason(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }

    private BigDecimal calculateTotalPrice(FootballField field, long durationMinutes) {
        return field.getPricePerHour()
                .multiply(BigDecimal.valueOf(durationMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
