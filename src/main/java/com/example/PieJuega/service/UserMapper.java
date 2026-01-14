package com.example.PieJuega.service;

import com.example.PieJuega.dto.UserResponseDTO;
import com.example.PieJuega.model.User;

import java.util.stream.Collectors;

public class UserMapper {

    public static UserResponseDTO toDTO(User user) {
        UserResponseDTO response = new UserResponseDTO();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRoles(user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()));
        return response;
    }
}
