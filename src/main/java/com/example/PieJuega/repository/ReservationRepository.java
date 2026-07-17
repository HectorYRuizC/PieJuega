package com.example.PieJuega.repository;

import com.example.PieJuega.model.Reservation;
import com.example.PieJuega.util.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByField_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            Long fieldId,
            Collection<ReservationStatus> statuses,
            LocalDateTime endAt,
            LocalDateTime startAt
    );

    boolean existsByField_IdAndStatusInAndStartAtAfter(
            Long fieldId,
            Collection<ReservationStatus> statuses,
            LocalDateTime startAt
    );

    List<Reservation> findByField_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            Long fieldId,
            Collection<ReservationStatus> statuses,
            LocalDateTime endAt,
            LocalDateTime startAt
    );

    @EntityGraph(attributePaths = {"field", "user"})
    List<Reservation> findByUser_IdOrderByStartAtDesc(Long userId);

    @EntityGraph(attributePaths = {"field", "user"})
    List<Reservation> findByStatusOrderByStartAtAsc(ReservationStatus status);

    @EntityGraph(attributePaths = {"field", "user"})
    List<Reservation> findAllByOrderByStartAtAsc();

    @Override
    @EntityGraph(attributePaths = {"field", "user"})
    Optional<Reservation> findById(Long id);
}
