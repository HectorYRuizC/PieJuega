package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.CreateTournamentRequestDTO;
import com.example.PieJuega.dto.request.JoinTournamentRequestDTO;
import com.example.PieJuega.dto.response.TournamentResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @GetMapping
    public List<TournamentResponseDTO> upcoming(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String cityCode,
            @RequestParam(required = false) String city
    ) {
        return tournamentService.getUpcoming(
                user.getId(), latitude, longitude, cityCode, city
        );
    }

    @GetMapping("/mine")
    public List<TournamentResponseDTO> mine(@AuthenticationPrincipal UserDetailsImpl user) {
        return tournamentService.getMyRequests(user.getId());
    }

    @GetMapping("/{tournamentId}")
    public TournamentResponseDTO tournament(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return tournamentService.getTournament(
                tournamentId, user.getId(), latitude, longitude
        );
    }

    @PostMapping
    public ResponseEntity<TournamentResponseDTO> create(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody CreateTournamentRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tournamentService.createTournament(user.getId(), request));
    }

    @PostMapping("/{tournamentId}/teams")
    public TournamentResponseDTO join(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long tournamentId,
            @Valid @RequestBody JoinTournamentRequestDTO request
    ) {
        return tournamentService.joinTournament(
                tournamentId, request.teamId(), user.getId()
        );
    }

    @DeleteMapping("/{tournamentId}/teams/{teamId}")
    public TournamentResponseDTO withdraw(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long tournamentId,
            @PathVariable Long teamId
    ) {
        return tournamentService.withdrawTeam(tournamentId, teamId, user.getId());
    }

    @PatchMapping("/{tournamentId}/cancel")
    public TournamentResponseDTO cancel(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long tournamentId
    ) {
        return tournamentService.cancelTournament(tournamentId, user.getId());
    }
}
