package com.example.PieJuega.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private LocalDate dateBirth;
    private Set<String> roles;
}