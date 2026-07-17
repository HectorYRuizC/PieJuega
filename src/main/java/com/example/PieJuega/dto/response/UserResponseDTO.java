package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.AuthProvider;
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
    private String photoUrl;
    private String city;
    private Double latitude;
    private Double longitude;
    private AuthProvider authProvider;
    private Set<String> roles;

}
