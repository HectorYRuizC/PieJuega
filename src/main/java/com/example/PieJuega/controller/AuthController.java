package com.example.PieJuega.controller;

import com.example.PieJuega.dto.*;
import com.example.PieJuega.exception.InvalidCredentialsException;
import com.example.PieJuega.model.User;
import com.example.PieJuega.service.AuthService;
import com.example.PieJuega.service.UserMapper;
import com.example.PieJuega.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.PieJuega.security.UserDetailsImpl;

import java.util.Set;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

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
                request.isAdmin()
        );
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }
}

