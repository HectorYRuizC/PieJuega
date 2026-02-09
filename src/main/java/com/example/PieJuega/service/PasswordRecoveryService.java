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

            // invalidar códigos anteriores
            codeRepository.findByEmailAndUsedFalse(email)
                    .forEach(c -> {
                        c.setUsed(true);
                        codeRepository.save(c);
                    });

            String code = String.format("%04d", new Random().nextInt(10000));

            PasswordResetCode resetCode = PasswordResetCode.builder()
                    .email(email)
                    .code(code)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();

            codeRepository.save(resetCode);
            emailService.sendPasswordRecoveryCode(email, code);
        });

        // SIEMPRE responder OK
    }

    /* ===============================
       2. VERIFICAR CÓDIGO
       =============================== */
    public String verifyCode(String email, String code) {

        PasswordResetCode resetCode = codeRepository
                .findByEmailAndCodeAndUsedFalse(email, code)
                .orElseThrow(() -> new RuntimeException("Código inválido"));

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Código expirado");
        }

        return jwtService.generatePasswordResetToken(email);
    }

    /* ===============================
       3. RESET CONTRASEÑA
       =============================== */
    public void resetPassword(String resetToken, PasswordResetDTO dto) {

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        // 1️ Validar token
        if (!jwtService.isTokenValid(resetToken)) {
            throw new RuntimeException("Token inválido o expirado");
        }

        // 2️ Validar que sea token de recuperación
        if (!jwtService.isPasswordResetToken(resetToken)) {
            throw new RuntimeException("Token no válido para recuperación de contraseña");
        }

        // 3️ Extraer email
        String email = jwtService.extractEmail(resetToken);

        // 4️ Buscar usuario
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 5️ Cambiar contraseña
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        // 6️ Marcar códigos como usados
        codeRepository.findByEmailAndUsedFalse(email)
                .forEach(code -> {
                    code.setUsed(true);
                    codeRepository.save(code);
                });
    }

}
