package com.example.PieJuega.repository;

import com.example.PieJuega.model.TeamMember;
import com.example.PieJuega.util.SquadRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUser_IdOrderByTeam_CreatedAtDesc(Long userId);

    List<TeamMember> findByTeam_IdOrderBySquadRoleAscSlotIndexAsc(Long teamId);

    long countByTeam_IdAndSquadRole(Long teamId, SquadRole squadRole);

    long countByTeam_Id(Long teamId);

    @Query("""
        SELECT member.team.id AS teamId, COUNT(member) AS memberCount
        FROM TeamMember member
        WHERE member.team.id IN :teamIds
        GROUP BY member.team.id
    """)
    List<TeamMemberCount> countByTeamIds(@Param("teamIds") Collection<Long> teamIds);

    interface TeamMemberCount {
        Long getTeamId();

        long getMemberCount();
    }
}
