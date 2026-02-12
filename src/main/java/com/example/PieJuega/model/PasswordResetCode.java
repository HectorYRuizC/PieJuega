package com.example.PieJuega.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String code;

    @Column(length = 500, unique = true)
    private String token;

    private boolean used;

    private boolean revoked;

    private int attempts;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

}
