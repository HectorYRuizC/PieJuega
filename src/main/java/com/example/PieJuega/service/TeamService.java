package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.CreateTeamRequestDTO;
import com.example.PieJuega.dto.request.TeamMemberRequestDTO;
import com.example.PieJuega.dto.response.PlayerSearchResponseDTO;
import com.example.PieJuega.dto.response.TeamMemberResponseDTO;
import com.example.PieJuega.dto.response.TeamResponseDTO;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.model.ChatRoom;
import com.example.PieJuega.model.FootballTeam;
import com.example.PieJuega.model.TeamMember;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FootballTeamRepository;
import com.example.PieJuega.repository.TeamMemberRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.PlayerPosition;
import com.example.PieJuega.util.SquadRole;
import com.example.PieJuega.util.TeamFormat;
import com.example.PieJuega.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamService {

    private static final int MAX_SUBSTITUTES = 12;
    private static final Map<TeamFormat, Set<String>> FORMATIONS = Map.of(
            TeamFormat.FIVE, Set.of("1-2-1", "2-1-1"),
            TeamFormat.SEVEN, Set.of("2-3-1", "3-2-1"),
            TeamFormat.EIGHT, Set.of("3-3-1", "2-3-2"),
            TeamFormat.ELEVEN, Set.of("4-3-3", "4-4-2", "3-5-2")
    );

    private final FootballTeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Transactional(readOnly = true)
    public List<PlayerSearchResponseDTO> searchPlayers(
            Long currentUserId,
            String query,
            Double latitude,
            Double longitude,
            String city
    ) {
        String normalized = query == null ? "" : query.trim();
        String normalizedCity = city == null ? "" : city.trim();
        return userRepository.searchPlayers(currentUserId, normalized, PageRequest.of(0, 30))
                .stream()
                .map(user -> new PlayerSearchResponseDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getPhotoUrl(),
                        user.getCity(),
                        GeoUtils.distanceKm(
                                latitude,
                                longitude,
                                user.getLatitude(),
                                user.getLongitude()
                        )
                ))
                .sorted(Comparator
                        .comparing((PlayerSearchResponseDTO player) -> normalizedCity.isEmpty()
                                || normalizedCity.equalsIgnoreCase(player.city()) ? 0 : 1)
                        .thenComparing(player -> player.distanceKm() == null
                                ? Double.MAX_VALUE
                                : player.distanceKm())
                        .thenComparing(PlayerSearchResponseDTO::username))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponseDTO> getMyTeams(Long userId) {
        return memberRepository.findByUser_IdOrderByTeam_CreatedAtDesc(userId)
                .stream()
                .map(TeamMember::getTeam)
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponseDTO getTeam(Long teamId, Long userId) {
        FootballTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
        boolean isMember = memberRepository.findByTeam_IdOrderBySquadRoleAscSlotIndexAsc(teamId)
                .stream()
                .anyMatch(member -> member.getUser().getId().equals(userId));
        if (!isMember) {
            throw new IllegalArgumentException("No perteneces a este equipo");
        }
        return toResponse(team);
    }

    @Transactional
    public TeamResponseDTO createTeam(Long ownerId, CreateTeamRequestDTO request) {
        User owner = getUser(ownerId);
        validateFormation(request.format(), request.formation());

        Map<Long, TeamMemberRequestDTO> requestedMembers = new LinkedHashMap<>();
        request.members().forEach(member -> {
            if (requestedMembers.putIfAbsent(member.userId(), member) != null) {
                throw new IllegalArgumentException("Un jugador no puede ocupar dos lugares");
            }
        });

        if (!requestedMembers.containsKey(ownerId)) {
            int slot = nextSubstituteSlot(requestedMembers.values().stream().toList());
            requestedMembers.put(ownerId, new TeamMemberRequestDTO(
                    ownerId,
                    SquadRole.SUBSTITUTE,
                    PlayerPosition.MF,
                    slot,
                    true
            ));
        }
        validateSquad(request.format(), requestedMembers.values().stream().toList());

        List<User> users = userRepository.findAllById(requestedMembers.keySet());
        if (users.size() != requestedMembers.size()) {
            throw new ResourceNotFoundException("Uno o más jugadores no existen");
        }
        Map<Long, User> usersById = new HashMap<>();
        users.forEach(user -> usersById.put(user.getId(), user));

        FootballTeam team = teamRepository.save(FootballTeam.builder()
                .name(request.name().trim())
                .description(blankToNull(request.description()))
                .city(blankToNull(request.city()))
                .shieldUrl(blankToNull(request.shieldUrl()))
                .primaryColor(defaultColor(request.primaryColor(), "#267A78"))
                .secondaryColor(defaultColor(request.secondaryColor(), "#ECFFFC"))
                .format(request.format())
                .formation(request.formation().trim())
                .owner(owner)
                .build());

        List<TeamMember> members = requestedMembers.values().stream()
                .map(member -> TeamMember.builder()
                        .team(team)
                        .user(usersById.get(member.userId()))
                        .squadRole(member.squadRole())
                        .position(member.position())
                        .slotIndex(member.slotIndex())
                        .captain(member.userId().equals(ownerId))
                        .build())
                .toList();
        memberRepository.saveAll(members);

        ChatRoom room = chatService.createTeamRoom(
                ownerId,
                team.getName(),
                team.getShieldUrl(),
                new LinkedHashSet<>(requestedMembers.keySet())
        );
        team.setChatRoom(room);
        teamRepository.save(team);
        return toResponse(team);
    }

    private void validateFormation(TeamFormat format, String formation) {
        if (!FORMATIONS.getOrDefault(format, Set.of()).contains(formation.trim())) {
            throw new IllegalArgumentException("La formación no corresponde al formato elegido");
        }
    }

    private void validateSquad(TeamFormat format, List<TeamMemberRequestDTO> members) {
        if (members.size() > format.getPlayersOnField() + MAX_SUBSTITUTES) {
            throw new IllegalArgumentException("La plantilla supera el máximo permitido");
        }

        Set<Integer> starterSlots = new LinkedHashSet<>();
        Set<Integer> substituteSlots = new LinkedHashSet<>();
        for (TeamMemberRequestDTO member : members) {
            if (member.squadRole() == SquadRole.STARTER) {
                if (member.slotIndex() >= format.getPlayersOnField()
                        || !starterSlots.add(member.slotIndex())) {
                    throw new IllegalArgumentException("Hay un lugar de titular inválido o repetido");
                }
            } else if (member.slotIndex() >= MAX_SUBSTITUTES
                    || !substituteSlots.add(member.slotIndex())) {
                throw new IllegalArgumentException("Hay un lugar de suplente inválido o repetido");
            }
        }
    }

    private int nextSubstituteSlot(List<TeamMemberRequestDTO> members) {
        Set<Integer> used = members.stream()
                .filter(member -> member.squadRole() == SquadRole.SUBSTITUTE)
                .map(TeamMemberRequestDTO::slotIndex)
                .collect(java.util.stream.Collectors.toSet());
        for (int slot = 0; slot < MAX_SUBSTITUTES; slot++) {
            if (!used.contains(slot)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("No hay espacio para el creador del equipo");
    }

    private TeamResponseDTO toResponse(FootballTeam team) {
        List<TeamMemberResponseDTO> members = memberRepository
                .findByTeam_IdOrderBySquadRoleAscSlotIndexAsc(team.getId())
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
                .toList();

        return new TeamResponseDTO(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCity(),
                team.getShieldUrl(),
                team.getPrimaryColor(),
                team.getSecondaryColor(),
                team.getFormat(),
                team.getFormation(),
                team.getOwner().getId(),
                team.getChatRoom() == null ? null : team.getChatRoom().getId(),
                members.size(),
                team.getCreatedAt(),
                members
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String defaultColor(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
