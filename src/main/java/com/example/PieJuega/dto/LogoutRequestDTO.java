package com.example.PieJuega.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LogoutRequestDTO {

    @NotBlank
    private String refreshToken;
}