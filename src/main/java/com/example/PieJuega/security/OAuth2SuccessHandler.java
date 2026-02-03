package com.example.PieJuega.security;

import com.example.PieJuega.security.JwtService;
import com.example.PieJuega.security.UserDetailsImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

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
                userDetails.getId()
        );

        // Respuesta simple en JSON
        response.setContentType("application/json");
        response.getWriter().write("{\"accessToken\":\"" + accessToken + "\",\"refreshToken\":\"" + refreshToken + "\"}");
    }
}