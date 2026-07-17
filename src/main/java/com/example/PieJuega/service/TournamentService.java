package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.CreateTournamentRequestDTO;
import com.example.PieJuega.dto.response.TournamentResponseDTO;
import com.example.PieJuega.dto.response.TournamentTeamResponseDTO;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.exception.TournamentConflictException;
import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.model.FootballTeam;
import com.example.PieJuega.model.Tournament;
import com.example.PieJuega.model.TournamentRegistration;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.repository.FootballTeamRepository;
import com.example.PieJuega.repository.TournamentRegistrationRepository;
import com.example.PieJuega.repository.TournamentRepository;
import com.example.PieJuega.repository.TeamMemberRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.GeoUtils;
import com.example.PieJuega.util.NotificationType;
import com.example.PieJuega.util.SquadRole;
import com.example.PieJuega.util.TournamentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private static final EnumSet<TournamentStatus> PUBLIC_STATUSES = EnumSet.of(
            TournamentStatus.OPEN_REGISTRATION,
            TournamentStatus.IN_PROGRESS,
            TournamentStatus.COMPLETED
    );

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final FootballFieldRepository fieldRepository;
    private final FootballTeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final CityCatalogService cityCatalogService;
    private final AppNotificationService notificationService;

    @Transactional(readOnly = true)
    public List<TournamentResponseDTO> getUpcoming(
            Long requesterId,
            Double latitude,
            Double longitude,
            String cityCode,
            String city
    ) {
        getUser(requesterId);
        var selectedCity = cityCatalogService.resolve(cityCode, city, null);
        if (selectedCity.isEmpty()) {
            return List.of();
        }

        return tournamentRepository
                .findUpcomingByCity(
                        EnumSet.of(TournamentStatus.OPEN_REGISTRATION),
                        LocalDateTime.now(),
                        selectedCity.get().code(),
                        selectedCity.get().name()
                )
                .stream()
                .map(tournament -> toResponse(tournament, latitude, longitude))
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentResponseDTO getTournament(
            Long tournamentId,
            Long requesterId,
            Double latitude,
            Double longitude
    ) {
        User requester = getUser(requesterId);
        Tournament tournament = getTournamentEntity(tournamentId);
        if (!PUBLIC_STATUSES.contains(tournament.getStatus())
                && !tournament.getCreator().getId().equals(requesterId)
                && !isAdmin(requester)) {
            throw new AccessDeniedException("No tienes acceso a este torneo");
        }
        return toResponse(tournament, latitude, longitude);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponseDTO> getMyRequests(Long userId) {
        getUser(userId);
        return tournamentRepository.findByCreator_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(tournament -> toResponse(tournament, null, null))
                .toList();
    }

    @Transactional
    public TournamentResponseDTO createTournament(Long creatorId, CreateTournamentRequestDTO request) {
        User creator = getUser(creatorId);
        FootballField field = fieldRepository.findById(request.fieldId())
                .filter(FootballField::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        validateDates(request.registrationDeadline(), request.startsAt());
        if (field.getFormat() != request.format()) {
            throw new IllegalArgumentException("El formato del torneo debe coincidir con la cancha");
        }

        Tournament tournament = tournamentRepository.save(Tournament.builder()
                .name(request.name().trim())
                .description(request.description().trim())
                .rules(blankToNull(request.rules()))
                .format(request.format())
                .field(field)
                .creator(creator)
                .startsAt(request.startsAt().withSecond(0).withNano(0))
                .registrationDeadline(request.registrationDeadline().withSecond(0).withNano(0))
                .maxTeams(request.maxTeams())
                .entryFee(request.entryFee())
                .prize(blankToNull(request.prize()))
                .status(TournamentStatus.PENDING_APPROVAL)
                .build());
        notificationService.notifyAdministrators(
                NotificationType.TOURNAMENT_REQUEST,
                "Nueva propuesta de torneo",
                creator.getUsername() + " propuso " + tournament.getName(),
                "/reservationsAdmin/tournaments/" + tournament.getId(),
                tournament.getId()
        );
        return toResponse(tournament, null, null);
    }

    @Transactional
    public TournamentResponseDTO joinTournament(Long tournamentId, Long teamId, Long userId) {
        User requester = getUser(userId);
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado"));
        FootballTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
        requireTeamOwner(team, userId);
        if (tournament.getStatus() != TournamentStatus.OPEN_REGISTRATION) {
            throw new IllegalArgumentException("El torneo no está recibiendo inscripciones");
        }
        if (!tournament.getRegistrationDeadline().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Las inscripciones ya cerraron");
        }
        if (team.getFormat() != tournament.getFormat()) {
            throw new IllegalArgumentException("El formato del equipo no coincide con el torneo");
        }
        long starters = teamMemberRepository.countByTeam_IdAndSquadRole(teamId, SquadRole.STARTER);
        if (starters < tournament.getFormat().getPlayersOnField()) {
            throw new IllegalArgumentException(
                    "Completa la plantilla titular antes de inscribir el equipo"
            );
        }
        if (registrationRepository.existsByTournament_IdAndTeam_Id(tournamentId, teamId)) {
            throw new TournamentConflictException("Este equipo ya está inscrito");
        }
        if (registrationRepository.countByTournament_Id(tournamentId) >= tournament.getMaxTeams()) {
            throw new TournamentConflictException("El torneo ya alcanzó el cupo máximo");
        }

        registrationRepository.save(TournamentRegistration.builder()
                .tournament(tournament)
                .team(team)
                .registeredBy(requester)
                .build());
        if (!tournament.getCreator().getId().equals(userId)) {
            notificationService.notifyUser(
                    tournament.getCreator(),
                    NotificationType.TOURNAMENT_REGISTRATION,
                    "Nuevo equipo inscrito",
                    team.getName() + " se unió a " + tournament.getName(),
                    "/tournaments/" + tournament.getId(),
                    tournament.getId()
            );
        }
        return toResponse(tournament, null, null);
    }

    @Transactional
    public TournamentResponseDTO withdrawTeam(Long tournamentId, Long teamId, Long userId) {
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado"));
        FootballTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
        requireTeamOwner(team, userId);
        if (tournament.getStatus() != TournamentStatus.OPEN_REGISTRATION
                || !tournament.getRegistrationDeadline().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ya no puedes retirar el equipo");
        }
        TournamentRegistration registration = registrationRepository
                .findByTournament_IdAndTeam_Id(tournamentId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("El equipo no está inscrito"));
        registrationRepository.delete(registration);
        return toResponse(tournament, null, null);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponseDTO> getAdminRequests(Long adminId, TournamentStatus status) {
        requireAdmin(adminId);
        TournamentStatus selected = status == null
                ? TournamentStatus.PENDING_APPROVAL
                : status;
        return tournamentRepository.findByStatusOrderByCreatedAtAsc(selected)
                .stream()
                .map(tournament -> toResponse(tournament, null, null))
                .toList();
    }

    @Transactional
    public TournamentResponseDTO approveTournament(Long tournamentId, Long adminId) {
        User admin = requireAdmin(adminId);
        Tournament tournament = getTournamentEntity(tournamentId);
        requirePending(tournament);
        validateDates(tournament.getRegistrationDeadline(), tournament.getStartsAt());
        tournament.setStatus(TournamentStatus.OPEN_REGISTRATION);
        tournament.setApprovedBy(admin);
        tournament.setRejectionReason(null);
        Tournament saved = tournamentRepository.save(tournament);
        notificationService.notifyUser(
                tournament.getCreator(),
                NotificationType.TOURNAMENT_APPROVED,
                "Torneo publicado",
                tournament.getName() + " ya está recibiendo equipos",
                "/tournaments/" + tournament.getId(),
                tournament.getId()
        );
        return toResponse(saved, null, null);
    }

    @Transactional
    public TournamentResponseDTO rejectTournament(Long tournamentId, Long adminId, String reason) {
        requireAdmin(adminId);
        Tournament tournament = getTournamentEntity(tournamentId);
        requirePending(tournament);
        tournament.setStatus(TournamentStatus.REJECTED);
        tournament.setRejectionReason(reason.trim());
        Tournament saved = tournamentRepository.save(tournament);
        notificationService.notifyUser(
                tournament.getCreator(),
                NotificationType.TOURNAMENT_REJECTED,
                "Torneo no aprobado",
                "Revisa la decisión sobre " + tournament.getName(),
                "/tournaments/" + tournament.getId(),
                tournament.getId()
        );
        return toResponse(saved, null, null);
    }

    @Transactional
    public TournamentResponseDTO cancelTournament(Long tournamentId, Long userId) {
        User requester = getUser(userId);
        Tournament tournament = getTournamentEntity(tournamentId);
        if (!tournament.getCreator().getId().equals(userId) && !isAdmin(requester)) {
            throw new AccessDeniedException("No puedes cancelar este torneo");
        }
        if (tournament.getStatus() != TournamentStatus.PENDING_APPROVAL
                && tournament.getStatus() != TournamentStatus.OPEN_REGISTRATION) {
            throw new IllegalArgumentException("Este torneo ya no se puede cancelar");
        }
        tournament.setStatus(TournamentStatus.CANCELLED);
        Tournament saved = tournamentRepository.save(tournament);
        if (isAdmin(requester)) {
            notificationService.notifyUser(
                    tournament.getCreator(),
                    NotificationType.TOURNAMENT_CANCELLED,
                    "Torneo cancelado",
                    tournament.getName() + " fue cancelado por administración",
                    "/tournaments/" + tournament.getId(),
                    tournament.getId()
            );
        } else {
            notificationService.notifyAdministrators(
                    NotificationType.TOURNAMENT_CANCELLED,
                    "Propuesta cancelada",
                    tournament.getCreator().getUsername() + " canceló " + tournament.getName(),
                    "/reservationsAdmin/tournaments/" + tournament.getId(),
                    tournament.getId()
            );
        }
        return toResponse(saved, null, null);
    }

    private TournamentResponseDTO toResponse(
            Tournament tournament,
            Double latitude,
            Double longitude
    ) {
        FootballField field = tournament.getField();
        List<TournamentTeamResponseDTO> teams = registrationRepository
                .findByTournament_IdOrderByCreatedAtAsc(tournament.getId())
                .stream()
                .map(registration -> new TournamentTeamResponseDTO(
                        registration.getTeam().getId(),
                        registration.getTeam().getName(),
                        registration.getTeam().getShieldUrl(),
                        registration.getTeam().getCity(),
                        registration.getCreatedAt()
                ))
                .toList();
        return new TournamentResponseDTO(
                tournament.getId(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getRules(),
                tournament.getFormat(),
                tournament.getStatus(),
                field.getId(),
                field.getName(),
                field.getAddress(),
                field.getCity(),
                field.getCityCode(),
                field.getLatitude(),
                field.getLongitude(),
                GeoUtils.distanceKm(latitude, longitude, field.getLatitude(), field.getLongitude()),
                tournament.getStartsAt(),
                tournament.getRegistrationDeadline(),
                tournament.getMaxTeams(),
                teams.size(),
                tournament.getEntryFee(),
                tournament.getPrize(),
                tournament.getCreator().getId(),
                tournament.getCreator().getUsername(),
                tournament.getRejectionReason(),
                tournament.getCreatedAt(),
                teams
        );
    }

    private void validateDates(LocalDateTime deadline, LocalDateTime startsAt) {
        LocalDateTime now = LocalDateTime.now();
        if (!deadline.isAfter(now)) {
            throw new IllegalArgumentException("El cierre de inscripciones debe ser futuro");
        }
        if (!startsAt.isAfter(deadline)) {
            throw new IllegalArgumentException("El torneo debe iniciar después del cierre de inscripciones");
        }
    }

    private void requirePending(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("La solicitud ya fue gestionada");
        }
    }

    private void requireTeamOwner(FootballTeam team, Long userId) {
        if (!team.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Solo el creador del equipo puede inscribirlo");
        }
    }

    private User requireAdmin(Long userId) {
        User user = getUser(userId);
        if (!isAdmin(user)) {
            throw new AccessDeniedException("Se requieren permisos de administrador");
        }
        return user;
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Tournament getTournamentEntity(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
