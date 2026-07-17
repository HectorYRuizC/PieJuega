package com.example.PieJuega.repository;

import com.example.PieJuega.model.TournamentRegistration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {

    @EntityGraph(attributePaths = {"team", "registeredBy"})
    List<TournamentRegistration> findByTournament_IdOrderByCreatedAtAsc(Long tournamentId);

    boolean existsByTournament_IdAndTeam_Id(Long tournamentId, Long teamId);

    long countByTournament_Id(Long tournamentId);

    Optional<TournamentRegistration> findByTournament_IdAndTeam_Id(Long tournamentId, Long teamId);
}
