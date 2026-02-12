package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.PasswordResetDTO;
import com.example.PieJuega.model.PasswordResetCode;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.PasswordResetCodeRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /* ===============================
       1. SOLICITAR CÓDIGO
       =============================== */
    public void requestRecovery(String email) {

        userRepository.findByEmail(email).ifPresent(user -> {

            // 🔥 Revocar TODO lo anterior
            codeRepository.findByEmailAndUsedFalseAndRevokedFalse(email)
                    .forEach(c -> {
                        c.setRevoked(true);
                        codeRepository.save(c);
                    });

            String code = String.format("%04d", new Random().nextInt(10000));

            PasswordResetCode resetCode = PasswordResetCode.builder()
                    .email(email)
                    .code(code)
                    .attempts(0)
                    .used(false)
                    .revoked(false)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(10)) // ⏱ fuerte
                    .build();

            codeRepository.save(resetCode);
            emailService.sendPasswordRecoveryCode(email, code);
        });

        // Siempre OK (anti user-enumeration)
    }


    /* ===============================
       2. VERIFICAR CÓDIGO
       =============================== */
    public String verifyCode(String email, String code) {

        PasswordResetCode resetCode = codeRepository
                .findFirstByEmailAndUsedFalseAndRevokedFalse(email)
                .orElse(null);

        if (resetCode == null) {
            throw new RuntimeException("Código inválido");
        }

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            resetCode.setRevoked(true);
            codeRepository.save(resetCode);
            throw new RuntimeException("Código inválido");
        }


        // 🔴 VALIDAR PRIMERO SI ES INCORRECTO
        if (!resetCode.getCode().equals(code)) {

            resetCode.setAttempts(resetCode.getAttempts() + 1);

            if (resetCode.getAttempts() >= 5) {
                resetCode.setRevoked(true);
            }

            codeRepository.save(resetCode);

            throw new RuntimeException("Código inválido");
        }

        // 🟢 Código correcto
        String token = jwtService.generatePasswordResetToken(email);

        resetCode.setToken(token);

        // 🔒 El código ya no puede reutilizarse
        resetCode.setCode(null);

        codeRepository.save(resetCode);

        return token;
    }


    /* ===============================
       3. RESET CONTRASEÑA
       =============================== */
    public void resetPassword(String resetToken, PasswordResetDTO dto) {

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        if (!jwtService.isTokenValid(resetToken)
                || !jwtService.isPasswordResetToken(resetToken)) {
            throw new RuntimeException("Token inválido");
        }

        PasswordResetCode resetCode = codeRepository
                .findByTokenAndUsedFalseAndRevokedFalse(resetToken)
                .orElseThrow(() -> new RuntimeException("Token ya usado o inválido"));

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        String email = jwtService.extractEmail(resetToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        // 🔒 invalidar TODO
        resetCode.setUsed(true);
        resetCode.setRevoked(true);
        codeRepository.save(resetCode);
    }


}
