package com.example.PieJuega.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class UserRegisterRequestDTO {
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    private String phone; // opcional

    @NotBlank
    private String password;

    private boolean admin; // true si será admin
}