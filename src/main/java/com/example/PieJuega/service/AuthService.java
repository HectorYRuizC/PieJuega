package com.example.PieJuega.service;

import com.example.PieJuega.dto.AuthResponseDTO;
import com.example.PieJuega.exception.InvalidCredentialsException;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.security.JwtService;
import com.example.PieJuega.security.UserDetailsImpl;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.example.PieJuega.mapper.UserMapper;


import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${google.client-id.android}")
    private String googleClientIdAndroid;

    @Value("${google.client-id.ios}")
    private String googleClientIdIos;


    @Value("${google.client-id.web}")
    private String webClientId;


    // luego en loginWithGoogle:
    private Set<String> googleClientIds;

    @PostConstruct
    public void init() {
        this.googleClientIds = Set.of(
                googleClientIdAndroid,
                googleClientIdIos,
                webClientId
        );
    }





    /* =========================
       LOGIN EMAIL / PHONE
       ========================= */
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

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow();

        return buildAuthResponse(user);
    }


    /* =========================
       LOGIN GOOGLE (MÓVIL)
       ========================= */
    public AuthResponseDTO loginWithGoogle(String idTokenString) {

        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        JacksonFactory.getDefaultInstance()
                )
                        .setAudience(List.of(
                                googleClientIdAndroid,
                                googleClientIdIos,
                                webClientId
                        ))
                        .build();


        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Token de Google inválido");
        }

        if (idToken == null) {
            throw new InvalidCredentialsException("Token de Google inválido");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, name,payload));

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return buildAuthResponse(user);
    }

    /* =========================
       REFRESH TOKEN
       ========================= */
    public AuthResponseDTO refresh(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        if (!jwtService.isTokenValid(refreshToken,email)) {
            throw new InvalidCredentialsException("Refresh token inválido");
        }

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

    /* =========================
       HELPERS
       ========================= */
    private AuthResponseDTO generateTokens(Long id, String email, Collection<?> authorities) {

        Set<String> roles = authorities.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(id, email, roles);
        String refreshToken = jwtService.generateRefreshToken(email);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    private User createGoogleUser(String email, String name, GoogleIdToken.Payload payload) {
        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER no existe"));

        // Fecha de cumpleaños (viene como YYYY-MM-DD)
        LocalDate birthDate = null;
        String birthDateStr = (String) payload.get("birthdate"); // este campo viene por el scope
        if (birthDateStr != null) {
            birthDate = LocalDate.parse(birthDateStr);
        }

        // Número de teléfono (opcional)
        String phone = payload.get("phone_number") != null
                ? payload.get("phone_number").toString()
                : ""; // si no existe, queda vacio


        User user = User.builder()
                .email(email)
                .username(name)
                .password("") // OAuth
                .dateBirth(birthDate)
                .phone(phone)
                .roles(Set.of(roleUser))
                .build();

        return userRepository.save(user);
    }



    private AuthResponseDTO buildAuthResponse(User user) {

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUser(UserMapper.toDTO(user));

        return response;
    }




}
