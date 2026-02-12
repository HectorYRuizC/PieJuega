package com.example.PieJuega.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
public class FacebookLoginRequestDTO {

    @NotBlank
    private String accessToken;
    private String phone;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateBirth;
    private String photoUrl;
}
