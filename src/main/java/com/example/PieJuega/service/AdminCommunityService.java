package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.AdminTeamResponseDTO;
import com.example.PieJuega.dto.response.AdminUserResponseDTO;
import com.example.PieJuega.dto.response.TeamMemberResponseDTO;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.model.FootballTeam;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FootballTeamRepository;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.repository.TeamMemberRepository;
import com.example.PieJuega.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCommunityService {

    private static final int MAX_RESULTS = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FootballTeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<AdminUserResponseDTO> getUsers(
            Long adminId,
            String query,
            Boolean active
    ) {
        requireAdmin(adminId);
        return userRepository.searchForAdmin(
                        normalize(query),
                        active,
                        PageRequest.of(0, MAX_RESULTS)
                )
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponseDTO setUserActive(
            Long adminId,
            Long userId,
            boolean active
    ) {
        requireAdmin(adminId);
        User user = getUser(userId);
        if (adminId.equals(userId) && !active) {
            throw new IllegalArgumentException("No puedes suspender tu propia cuenta");
        }
        if (!active && isAdmin(user) && userRepository.countActiveAdministrators() <= 1) {
            throw new IllegalArgumentException("Debe permanecer al menos un administrador activo");
        }
        user.setActive(active);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponseDTO setAdministrator(
            Long adminId,
            Long userId,
            boolean administrator
    ) {
        requireAdmin(adminId);
        User user = getUser(userId);
        if (adminId.equals(userId) && !administrator) {
            throw new IllegalArgumentException("No puedes retirar tu propio rol de administrador");
        }
        if (!administrator
                && user.isActive()
                && isAdmin(user)
                && userRepository.countActiveAdministrators() <= 1) {
            throw new IllegalArgumentException("Debe permanecer al menos un administrador activo");
        }

        if (administrator) {
            Role role = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new ResourceNotFoundException("Rol administrador no encontrado"));
            user.getRoles().add(role);
        } else {
            user.getRoles().removeIf(role -> "ROLE_ADMIN".equals(role.getName()));
        }
        return toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<AdminTeamResponseDTO> getTeams(
            Long adminId,
            String query,
            Boolean active
    ) {
        requireAdmin(adminId);
        List<FootballTeam> teams = teamRepository.searchForAdmin(
                        normalize(query),
                        active,
                        PageRequest.of(0, MAX_RESULTS)
                );
        if (teams.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> memberCounts = memberRepository.countByTeamIds(
                        teams.stream().map(FootballTeam::getId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        TeamMemberRepository.TeamMemberCount::getTeamId,
                        TeamMemberRepository.TeamMemberCount::getMemberCount
                ));
        return teams.stream()
                .map(team -> toTeamResponse(
                        team,
                        false,
                        memberCounts.getOrDefault(team.getId(), 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminTeamResponseDTO getTeam(Long adminId, Long teamId) {
        requireAdmin(adminId);
        return toTeamResponse(getTeamEntity(teamId), true, null);
    }

    @Transactional
    public AdminTeamResponseDTO setTeamActive(
            Long adminId,
            Long teamId,
            boolean active
    ) {
        requireAdmin(adminId);
        FootballTeam team = getTeamEntity(teamId);
        team.setActive(active);
        return toTeamResponse(teamRepository.save(team), true, null);
    }

    private AdminUserResponseDTO toUserResponse(User user) {
        return new AdminUserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getDateBirth(),
                user.getPhotoUrl(),
                user.getCity(),
                user.getDepartment(),
                user.getAuthProvider(),
                user.isVerified(),
                user.isActive(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
        );
    }

    private AdminTeamResponseDTO toTeamResponse(
            FootballTeam team,
            boolean includeMembers,
            Long knownMemberCount
    ) {
        List<TeamMemberResponseDTO> members = includeMembers
                ? memberRepository.findByTeam_IdOrderBySquadRoleAscSlotIndexAsc(team.getId())
                .stream()
                .map(member -> new TeamMemberResponseDTO(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getUser().getPhotoUrl(),
                        member.getSquadRole(),
                        member.getPosition(),
                        member.getSlotIndex(),
                        member.isCaptain()
                ))
                .toList()
                : List.of();
        long memberCount = includeMembers ? members.size() : knownMemberCount;
        return new AdminTeamResponseDTO(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCity(),
                team.getShieldUrl(),
                team.getPrimaryColor(),
                team.getSecondaryColor(),
                team.getFormat(),
                team.getFormation(),
                team.isActive(),
                team.getOwner().getId(),
                team.getOwner().getUsername(),
                team.getChatRoom() == null ? null : team.getChatRoom().getId(),
                memberCount,
                team.getCreatedAt(),
                members
        );
    }

    private User requireAdmin(Long userId) {
        User user = getUser(userId);
        if (!user.isActive() || !isAdmin(user)) {
            throw new AccessDeniedException("Acceso exclusivo para administradores");
        }
        return user;
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private FootballTeam getTeamEntity(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
    }

    private String normalize(String query) {
        return query == null ? "" : query.trim();
    }
}
