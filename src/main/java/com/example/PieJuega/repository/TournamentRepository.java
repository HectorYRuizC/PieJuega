package com.example.PieJuega.repository;

import com.example.PieJuega.model.Tournament;
import com.example.PieJuega.util.TournamentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    boolean existsByField_IdAndStatusIn(
            Long fieldId,
            Collection<TournamentStatus> statuses
    );

    @EntityGraph(attributePaths = {"field", "creator"})
    @Query("""
        SELECT t FROM Tournament t
        WHERE t.status IN :statuses
          AND t.startsAt > :startsAt
          AND (
            t.field.cityCode = :cityCode
            OR (t.field.cityCode IS NULL AND LOWER(t.field.city) = LOWER(:cityName))
          )
        ORDER BY t.startsAt ASC
    """)
    List<Tournament> findUpcomingByCity(
            Collection<TournamentStatus> statuses,
            LocalDateTime startsAt,
            String cityCode,
            String cityName
    );

    @EntityGraph(attributePaths = {"field", "creator"})
    List<Tournament> findByCreator_IdOrderByCreatedAtDesc(Long creatorId);

    @EntityGraph(attributePaths = {"field", "creator"})
    List<Tournament> findByStatusOrderByCreatedAtAsc(TournamentStatus status);

    @EntityGraph(attributePaths = {"field", "creator"})
    @Override
    Optional<Tournament> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tournament t WHERE t.id = :id")
    Optional<Tournament> findByIdForUpdate(Long id);
}
