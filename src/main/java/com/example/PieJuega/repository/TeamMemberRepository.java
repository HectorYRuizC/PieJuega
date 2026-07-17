package com.example.PieJuega.repository;

import com.example.PieJuega.model.TeamMember;
import com.example.PieJuega.util.SquadRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUser_IdOrderByTeam_CreatedAtDesc(Long userId);

    List<TeamMember> findByTeam_IdOrderBySquadRoleAscSlotIndexAsc(Long teamId);

    long countByTeam_IdAndSquadRole(Long teamId, SquadRole squadRole);
}
