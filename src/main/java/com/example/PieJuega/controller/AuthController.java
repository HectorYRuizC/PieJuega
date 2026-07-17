package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.PasswordRecoveryVerifyDTO;
import com.example.PieJuega.dto.request.PasswordResetDTO;
import com.example.PieJuega.dto.request.*;
import com.example.PieJuega.dto.response.AuthResponseDTO;
import com.example.PieJuega.dto.response.PhoneExistResponseDTO;
import com.example.PieJuega.dto.response.UserResponseDTO;
import com.example.PieJuega.model.User;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.AuthService;
import com.example.PieJuega.mapper.UserMapper;
import com.example.PieJuega.service.PasswordRecoveryService;
import com.example.PieJuega.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final PasswordRecoveryService service;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody UserLoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request.getIdentifier(), request.getPassword()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        String refreshToken = request.getRefreshToken();
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegisterRequestDTO request) {
        User user = userService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getPhone(),
                request.getDateBirth(),
                request.getCity()
        );
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> googleLogin(
            @Valid @RequestBody GoogleLoginRequestDTO request) {

        return ResponseEntity.ok(
                authService.loginWithGoogle(request.getIdToken(), request.getDateBirth(), request.getPhone(),request.getPhotoUrl())
        );
    }

    @PostMapping("/facebook")
    public ResponseEntity<AuthResponseDTO> loginWithFacebook(
            @RequestBody @Valid FacebookLoginRequestDTO dto
    ) {
        return ResponseEntity.ok(
                authService.loginWithFacebook(dto.getAccessToken(), dto.getPhotoUrl())
        );
    }





    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody LogoutRequestDTO request
    ) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }







    @PostMapping("/recovety")
    public ResponseEntity<?> request(
            @RequestBody @Valid PasswordRecoveryRequestDTO dto
    ) {
        service.requestRecovery(dto.getEmail());
        return ResponseEntity.ok(
                Map.of("message", "Si el correo existe, se enviará un código")
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestBody @Valid PasswordRecoveryVerifyDTO dto
    ) {
        String resetToken = service.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(Map.of("resetToken", resetToken));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(
            @RequestBody @Valid PasswordResetDTO dto
    ) {
        service.resetPassword(dto.getToken(), dto);
        return ResponseEntity.noContent().build();
    }




    @GetMapping("/verifyEmail")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Cuenta verificada correctamente");
    }




    @PostMapping("/VerificationByEmail")
    public ResponseEntity<String> requestVerification(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        authService.sendVerificationEmail(userDetails.getUsername());

        return ResponseEntity.ok("Correo de verificación enviado");
    }



    @PostMapping("/resetByPhone")
    public ResponseEntity<?> resetByPhone(
            @RequestBody @Valid ResetByPhoneDTO dto
    ) {
        service.resetPasswordByPhone( dto);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/phoneExist/{phone}")
    public ResponseEntity<PhoneExistResponseDTO> phoneExist(
            @PathVariable String phone
    ) {
        return ResponseEntity.ok(userService.getPhoneInfo(phone));
    }






}
