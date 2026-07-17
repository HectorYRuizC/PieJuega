package com.example.PieJuega.repository;

import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.util.TeamFormat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FootballFieldRepository extends JpaRepository<FootballField, Long> {

    @Query("""
        SELECT f FROM FootballField f
        WHERE f.active = true
          AND (:format IS NULL OR f.format = :format)
          AND (
            :query = ''
            OR LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(f.city) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(f.address) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY f.rating DESC, f.name ASC
    """)
    List<FootballField> searchActive(String query, TeamFormat format);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FootballField f WHERE f.id = :id AND f.active = true")
    Optional<FootballField> findActiveByIdForUpdate(Long id);
}
