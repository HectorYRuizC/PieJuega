package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.AuthResponseDTO;
import com.example.PieJuega.exception.InvalidCredentialsException;
import com.example.PieJuega.model.RevokedToken;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.RevokedTokenRepository;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.util.AuthProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.example.PieJuega.mapper.UserMapper;
import org.springframework.web.client.RestTemplate;


import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final EmailService emailService;

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
    public AuthResponseDTO loginWithGoogle(String idTokenString, LocalDate dateBirth, String phone, String photoUrl) {

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
                .orElseGet(() -> createGoogleUser(email, name,dateBirth,phone,photoUrl));

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return buildAuthResponse(user);
    }




    public AuthResponseDTO loginWithFacebook(String accessToken, String photoUr) {

        String url = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + accessToken;

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> body = response.getBody();

        if (body == null || body.get("email") == null) {
            throw new RuntimeException("Facebook no proporcionó email válido");
        }

        String email = body.get("email").toString();
        String name = body.get("name") != null ? body.get("name").toString() : "Facebook User";

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createFacebookUser(email, name, photoUr));

        return buildAuthResponse(user);
    }





















    /* =========================
       REFRESH TOKEN
       ========================= */
    public AuthResponseDTO refresh(String refreshToken) {

        // 1️ Verificar si el token fue revocado
        if (revokedTokenRepository.existsByToken(refreshToken)) {
            throw new InvalidCredentialsException("Refresh token revocado");
        }

        // 2️ Validación básica del token (firma, expiración)
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidCredentialsException("Refresh token inválido");
        }

        // 3️ Extraer userId del token
        Long userId = jwtService.extractUserId(refreshToken);

        // 4️ Buscar usuario por ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        // 5️ Validación fuerte (token ↔ usuario)
        if (!jwtService.isTokenValid(refreshToken, user.getId())) {
            throw new InvalidCredentialsException("Refresh token inválido");
        }

        // 6️ Generar nuevo access token (refresh se reutiliza)
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



    private User createGoogleUser(String email, String name, LocalDate dateBirth, String phone, String photoUrl) {
        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER no existe"));


        User user = User.builder()
                .email(email)
                .username(name)
                .password("") // ✅ correcto para OAuth
                .authProvider(AuthProvider.GOOGLE) // 🔑 CLAVE
                .dateBirth(dateBirth)
                .phone(phone)
                .photoUrl(photoUrl)
                .roles(Set.of(roleUser))
                .build();

        return userRepository.save(user);
    }




//    private User createFacebookUser(String email, String name, LocalDate dateBirth, String phone, String photoUrl) {
//        Role roleUser = roleRepository.findByName("ROLE_USER")
//                .orElseThrow(() -> new RuntimeException("ROLE_USER no existe"));
//
//
//        User user = User.builder()
//                .email(email)
//                .username(name)
//                .password("") // ✅ correcto para OAuth
//                .authProvider(AuthProvider.FACEBOOK) // 🔑 CLAVE
//                .dateBirth(dateBirth)
//                .phone(phone)
//                .photoUrl(photoUrl)
//                .roles(Set.of(roleUser))
//                .build();
//
//        return userRepository.save(user);
//    }


    private User createFacebookUser(String email, String name, String photoUrl ) {

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER no existe"));

        User user = User.builder()
                .email(email)
                .username(name)
                .password("") // OAuth
                .photoUrl(photoUrl)
                .authProvider(AuthProvider.FACEBOOK)
                .roles(Set.of(roleUser))
                .build();

        return userRepository.save(user);
    }















    public void logout(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            return; // token inválido → nada que revocar
        }

        if (revokedTokenRepository.existsByToken(refreshToken)) {
            return; // ya fue revocado
        }

        revokedTokenRepository.save(
                RevokedToken.builder()
                        .token(refreshToken)
                        .revokedAt(Instant.now())
                        .build()
        );
    }




    public void sendVerificationEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.isVerified()) {
            throw new RuntimeException("El usuario ya está verificado");
        }

        String token = jwtService.generateEmailVerificationToken(email);

        String verificationLink = "http://localhost:8080/api/auth/verifyEmail?token=" + token;

        emailService.sendEmail(
                email,
                "Verifica tu cuenta",
                "Haz clic en el siguiente enlace para verificar tu cuenta:\n" + verificationLink
        );
    }




    public void verifyEmail(String token) {

        try {

            if (!jwtService.isEmailVerificationToken(token)) {
                throw new RuntimeException("Token inválido");
            }

            String email = jwtService.extractEmail(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (user.isVerified()) {
                throw new RuntimeException("Usuario ya verificado");
            }

            user.setVerified(true);
            userRepository.save(user);

        } catch (ExpiredJwtException e) {
            throw new RuntimeException("El enlace ha expirado");
        }
    }










}
