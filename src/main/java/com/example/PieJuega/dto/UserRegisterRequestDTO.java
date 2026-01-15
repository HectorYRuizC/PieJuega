package com.example.PieJuega.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;


@Data
public class UserRegisterRequestDTO {
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;


    private String phone; // opcional


    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateBirth;

    @NotBlank
    private String password;

    @NotNull
    private boolean admin; // true si será admin
}