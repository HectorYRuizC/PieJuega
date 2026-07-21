package com.example.PieJuega.controller;

import com.example.PieJuega.dto.response.AdminTeamResponseDTO;
import com.example.PieJuega.dto.response.AdminUserResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.AdminCommunityService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityService communityService;

    @GetMapping("/users")
    public List<AdminUserResponseDTO> users(
            @AuthenticationPrincipal UserDetailsImpl admin,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) Boolean active
    ) {
        return communityService.getUsers(admin.getId(), query, active);
    }

    @PatchMapping("/users/{userId}/active")
    public AdminUserResponseDTO setUserActive(
            @AuthenticationPrincipal UserDetailsImpl admin,
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request
    ) {
        return communityService.setUserActive(
                admin.getId(),
                userId,
                requiredBoolean(request, "active", "El estado del usuario es obligatorio")
        );
    }

    @PatchMapping("/users/{userId}/administrator")
    public AdminUserResponseDTO setAdministrator(
            @AuthenticationPrincipal UserDetailsImpl admin,
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request
    ) {
        return communityService.setAdministrator(
                admin.getId(),
                userId,
                requiredBoolean(request, "administrator", "El rol es obligatorio")
        );
    }

    @GetMapping("/teams")
    public List<AdminTeamResponseDTO> teams(
            @AuthenticationPrincipal UserDetailsImpl admin,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) Boolean active
    ) {
        return communityService.getTeams(admin.getId(), query, active);
    }

    @GetMapping("/teams/{teamId}")
    public AdminTeamResponseDTO team(
            @AuthenticationPrincipal UserDetailsImpl admin,
            @PathVariable Long teamId
    ) {
        return communityService.getTeam(admin.getId(), teamId);
    }

    @PatchMapping("/teams/{teamId}/active")
    public AdminTeamResponseDTO setTeamActive(
            @AuthenticationPrincipal UserDetailsImpl admin,
            @PathVariable Long teamId,
            @RequestBody Map<String, Boolean> request
    ) {
        return communityService.setTeamActive(
                admin.getId(),
                teamId,
                requiredBoolean(request, "active", "El estado del equipo es obligatorio")
        );
    }

    private boolean requiredBoolean(
            Map<String, Boolean> request,
            String key,
            String message
    ) {
        Boolean value = request.get(key);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
