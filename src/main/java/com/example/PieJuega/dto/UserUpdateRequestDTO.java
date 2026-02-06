package com.example.PieJuega.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserUpdateRequestDTO {
    private String username;
    private String email;
    private String phone;
    private LocalDate dateBirth;
    private String photoUrl;
}