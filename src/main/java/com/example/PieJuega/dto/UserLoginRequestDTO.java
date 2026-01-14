package com.example.PieJuega.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class UserLoginRequestDTO {

    @NotBlank
    private String identifier;

    @NotBlank
    private String password;
}
