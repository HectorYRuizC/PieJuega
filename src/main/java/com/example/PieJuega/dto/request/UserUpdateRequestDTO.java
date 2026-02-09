package com.example.PieJuega.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Data
public class UserUpdateRequestDTO {
    private String username;
    @Email
    private String email;
    private String phone;
    private LocalDate dateBirth;
    @URL(message = "URL de imagen inválida")
    private String photoUrl;
}