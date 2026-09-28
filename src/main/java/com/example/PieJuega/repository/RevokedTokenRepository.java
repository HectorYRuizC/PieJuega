package com.example.PieJuega.repository;

import com.example.PieJuega.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    Optional<RevokedToken> findByToken(String token);
    boolean existsByToken(String token);

    /** Borra tokens revocados antes de la fecha indicada (limpieza programada). */
    void deleteByRevokedAtBefore(Instant cutoff);
}
