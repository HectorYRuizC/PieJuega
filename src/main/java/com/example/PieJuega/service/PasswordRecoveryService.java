package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.PasswordResetDTO;
import com.example.PieJuega.dto.request.ResetByPhoneDTO;
import com.example.PieJuega.model.PasswordResetCode;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.PasswordResetCodeRepository;
import com.example.PieJuega.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.security.SecureRandom;



@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private static final SecureRandom secureRandom = new SecureRandom();


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

            int number = 100000 + secureRandom.nextInt(900000);
            String rawCode = String.valueOf(number);

            // 🔐 Hashear el código antes de guardarlo
            String hashedCode = passwordEncoder.encode(rawCode);


            PasswordResetCode resetCode = PasswordResetCode.builder()
                    .email(email)
                    .code(hashedCode)
                    .attempts(0)
                    .used(false)
                    .revoked(false)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(10)) // ⏱ fuerte
                    .build();

            codeRepository.save(resetCode);
            emailService.sendPasswordRecoveryCode(email, rawCode);
        });

        // Siempre OK (anti user-enumeration)
    }


    /* ===============================
       2. VERIFICAR CÓDIGO
       =============================== */

    public String verifyCode(String email, String code) {

        PasswordResetCode resetCode = codeRepository
                .findFirstByEmailAndUsedFalseAndRevokedFalse(email)
                .orElseThrow(() -> new RuntimeException("Código inválido"));

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            resetCode.setRevoked(true);
            codeRepository.save(resetCode);
            throw new RuntimeException("Código inválido");
        }


        //  VALIDAR PRIMERO SI ES INCORRECTO
        if (!passwordEncoder.matches(code, resetCode.getCode())) {

            resetCode.setAttempts(resetCode.getAttempts() + 1);

            if (resetCode.getAttempts() >= 5) {
                resetCode.setRevoked(true);
            }

            codeRepository.save(resetCode);

            throw new RuntimeException("Código inválido");
        }

        //  Código correcto
        String token = jwtService.generatePasswordResetToken(email);

        resetCode.setToken(token);



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


    /* ===============================
      3. RESET CONTRASEÑA por PHONE
      =============================== */
    public void resetPasswordByPhone( ResetByPhoneDTO dto) {

        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        User user = userRepository.findByPhone(dto.getPhone())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);


    }








}
