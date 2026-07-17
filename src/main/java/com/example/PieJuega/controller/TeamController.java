package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.CreateTeamRequestDTO;
import com.example.PieJuega.dto.response.PlayerSearchResponseDTO;
import com.example.PieJuega.dto.response.TeamResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/players")
    public List<PlayerSearchResponseDTO> searchPlayers(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String city
    ) {
        return teamService.searchPlayers(
                user.getId(), query, latitude, longitude, city
        );
    }

    @GetMapping("/mine")
    public List<TeamResponseDTO> myTeams(@AuthenticationPrincipal UserDetailsImpl user) {
        return teamService.getMyTeams(user.getId());
    }

    @GetMapping("/{teamId}")
    public TeamResponseDTO team(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long teamId
    ) {
        return teamService.getTeam(teamId, user.getId());
    }

    @PostMapping
    public ResponseEntity<TeamResponseDTO> createTeam(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody CreateTeamRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(user.getId(), request));
    }
}
