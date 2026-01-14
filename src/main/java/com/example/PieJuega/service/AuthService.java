package com.example.PieJuega.service;

import com.example.PieJuega.dto.AuthResponseDTO;
import com.example.PieJuega.exception.InvalidCredentialsException;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.security.JwtService;
import com.example.PieJuega.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthResponseDTO login(String identifier, String password) {

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, password)
            );
        } catch (Exception e) {
            throw new InvalidCredentialsException("Credenciales incorrectas");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(
                userDetails.getId(),
                userDetails.getUsername(),
                roles
        );

        String refreshToken = jwtService.generateRefreshToken(
                userDetails.getUsername()
        );

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    public AuthResponseDTO refresh(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);

        if (!jwtService.isTokenValid(refreshToken, email)) {
            throw new InvalidCredentialsException("Refresh token inválido");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }
}

