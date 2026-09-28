package com.example.PieJuega.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JwtServiceTests {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test_secret_key_with_enough_length_for_hmac_256");
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 900_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 7L * 24 * 60 * 60 * 1000);
    }

    @Test
    void accessTokenIsRecognizedAsAccessAndNotAsRefresh() {
        String accessToken = jwtService.generateAccessToken(1L, "user@test.com", Set.of("ROLE_USER"));

        assertTrue(jwtService.isTokenValid(accessToken));
        assertTrue(jwtService.isAccessToken(accessToken));
        assertFalse(jwtService.isRefreshToken(accessToken));
    }

    @Test
    void refreshTokenIsRecognizedAsRefreshAndNotAsAccess() {
        String refreshToken = jwtService.generateRefreshToken(1L);

        assertTrue(jwtService.isTokenValid(refreshToken));
        assertTrue(jwtService.isRefreshToken(refreshToken));
        assertFalse(jwtService.isAccessToken(refreshToken));
    }

    @Test
    void passwordResetTokenIsNeitherAccessNorRefresh() {
        String resetToken = jwtService.generatePasswordResetToken("user@test.com");

        assertTrue(jwtService.isPasswordResetToken(resetToken));
        assertFalse(jwtService.isAccessToken(resetToken));
        assertFalse(jwtService.isRefreshToken(resetToken));
    }

    @Test
    void emailVerificationTokenIsNeitherAccessNorRefresh() {
        String verificationToken = jwtService.generateEmailVerificationToken("user@test.com");

        assertTrue(jwtService.isEmailVerificationToken(verificationToken));
        assertFalse(jwtService.isAccessToken(verificationToken));
        assertFalse(jwtService.isRefreshToken(verificationToken));
    }
}
