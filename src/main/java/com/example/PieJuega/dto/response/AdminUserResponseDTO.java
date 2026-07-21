package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.AuthProvider;

import java.time.LocalDate;
import java.util.Set;

public record AdminUserResponseDTO(
        Long id,
        String username,
        String email,
        String phone,
        LocalDate dateBirth,
        String photoUrl,
        String city,
        String department,
        AuthProvider authProvider,
        boolean verified,
        boolean active,
        Set<String> roles
) {
}
