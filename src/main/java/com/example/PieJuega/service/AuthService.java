package com.example.PieJuega.service;

import com.example.PieJuega.dto.AuthResponseDTO;
import com.example.PieJuega.exception.InvalidCredentialsException;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.security.JwtService;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.util.AuthProvider;
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
    public AuthResponseDTO loginWithGoogle(String idTokenString, LocalDate dateBirth, String phone) {

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
                .orElseGet(() -> createGoogleUser(email, name,dateBirth,phone));

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
        // Validación básica del token
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidCredentialsException("Refresh token inválido");
        }

        // Extraer userId del token
        Long userId = jwtService.extractUserId(refreshToken);

        // Buscar usuario por ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        // Validación fuerte (token ↔ usuario)
        if (!jwtService.isTokenValid(refreshToken, user.getId())) {
            throw new InvalidCredentialsException("Refresh token inválido");
        }

        // Usar buildAuthResponse para generar tokens y el DTO
        return buildAuthResponse(user);
    }

    /* =========================
       HELPERS - genera el token
       ========================= */
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

        String refreshToken = jwtService.generateRefreshToken(user.getId());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUser(UserMapper.toDTO(user));

        return response;
    }



    private User createGoogleUser(String email, String name, LocalDate dateBirth, String phone) {
        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER no existe"));


        User user = User.builder()
                .email(email)
                .username(name)
                .password(null) // ✅ correcto para OAuth
                .authProvider(AuthProvider.GOOGLE) // 🔑 CLAVE
                .dateBirth(dateBirth)
                .phone(phone)
                .roles(Set.of(roleUser))
                .build();

        return userRepository.save(user);
    }








}
