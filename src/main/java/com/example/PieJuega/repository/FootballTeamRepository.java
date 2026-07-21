package com.example.PieJuega.repository;

import com.example.PieJuega.model.FootballTeam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface FootballTeamRepository extends JpaRepository<FootballTeam, Long> {

    @EntityGraph(attributePaths = "owner")
    @Query("""
        SELECT team FROM FootballTeam team
        WHERE (:active IS NULL OR team.active = :active)
          AND (
            :query = ''
            OR LOWER(team.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(team.city, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(team.owner.username) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY team.createdAt DESC
    """)
    List<FootballTeam> searchForAdmin(String query, Boolean active, Pageable pageable);

    long countByOwner_Id(Long ownerId);

    @Query("""
        SELECT team.chatRoom.id FROM FootballTeam team
        WHERE team.active = false AND team.chatRoom IS NOT NULL
    """)
    Set<Long> findArchivedChatRoomIds();

    boolean existsByChatRoom_IdAndActiveFalse(Long roomId);
}
