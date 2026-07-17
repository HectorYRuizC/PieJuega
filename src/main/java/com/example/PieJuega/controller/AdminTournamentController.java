package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.RejectTournamentRequestDTO;
import com.example.PieJuega.dto.response.TournamentResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.TournamentService;
import com.example.PieJuega.util.TournamentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tournaments")
@RequiredArgsConstructor
public class AdminTournamentController {

    private final TournamentService tournamentService;

    @GetMapping
    public List<TournamentResponseDTO> requests(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(required = false) TournamentStatus status
    ) {
        return tournamentService.getAdminRequests(user.getId(), status);
    }

    @PatchMapping("/{tournamentId}/approve")
    public TournamentResponseDTO approve(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long tournamentId
    ) {
        return tournamentService.approveTournament(tournamentId, user.getId());
    }

    @PatchMapping("/{tournamentId}/reject")
    public TournamentResponseDTO reject(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long tournamentId,
            @Valid @RequestBody RejectTournamentRequestDTO request
    ) {
        return tournamentService.rejectTournament(
                tournamentId, user.getId(), request.reason()
        );
    }
}
