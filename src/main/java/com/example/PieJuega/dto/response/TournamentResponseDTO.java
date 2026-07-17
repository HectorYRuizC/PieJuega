package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.TeamFormat;
import com.example.PieJuega.util.TournamentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TournamentResponseDTO(
        Long id,
        String name,
        String description,
        String rules,
        TeamFormat format,
        TournamentStatus status,
        Long fieldId,
        String fieldName,
        String fieldAddress,
        String city,
        Double latitude,
        Double longitude,
        Double distanceKm,
        LocalDateTime startsAt,
        LocalDateTime registrationDeadline,
        int maxTeams,
        int registeredTeamsCount,
        BigDecimal entryFee,
        String prize,
        Long creatorId,
        String creatorName,
        String rejectionReason,
        LocalDateTime createdAt,
        List<TournamentTeamResponseDTO> teams
) {
}
