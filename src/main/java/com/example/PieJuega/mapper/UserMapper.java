package com.example.PieJuega.mapper;

import com.example.PieJuega.dto.response.UserResponseDTO;
import com.example.PieJuega.model.User;

import java.util.stream.Collectors;

public class UserMapper {

    public static UserResponseDTO toDTO(User user) {
        UserResponseDTO response = new UserResponseDTO();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setDateBirth(user.getDateBirth());
        response.setAuthProvider(user.getAuthProvider()); // 🔑 AQUÍ
        response.setPhotoUrl(user.getPhotoUrl());
        response.setCity(user.getCity());
        response.setDepartment(user.getDepartment());
        response.setCityCode(user.getCityCode());
        response.setLatitude(user.getLatitude());
        response.setLongitude(user.getLongitude());
        response.setRoles(user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()));
        return response;
    }
}
