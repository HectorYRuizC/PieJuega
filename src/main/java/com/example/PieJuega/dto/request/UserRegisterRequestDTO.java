package com.example.PieJuega.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;


@Data
public class UserRegisterRequestDTO {
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;


    private String phone;

    @Size(max = 80)
    private String city;


    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateBirth;

    @NotBlank
    private String password;

    @NotNull
    private boolean admin;
}
