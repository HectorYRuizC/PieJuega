package com.example.PieJuega.service;

import com.example.PieJuega.exception.InvalidCredentialsException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Verifies Firebase ID tokens to prove that a phone number was verified
 * through Firebase Phone Auth (SMS) before allowing sensitive operations
 * such as password resets.
 */
@Component
@Slf4j
public class FirebasePhoneVerifier {

    private static final String APP_NAME = "piejuega-auth";

    private final String projectId;

    public FirebasePhoneVerifier(@Value("${firebase.project-id}") String projectId) {
        this.projectId = projectId;
    }

    /**
     * Verifies a Firebase ID token and returns the verified phone number
     * in E.164 format (e.g. +573001234567).
     *
     * <p>Fails closed: if Firebase is not configured, or the token is
     * invalid/expired, an {@link InvalidCredentialsException} is thrown.</p>
     */
    public String verifyPhoneIdToken(String idToken) {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance(getFirebaseApp());
            FirebaseToken decoded = auth.verifyIdToken(idToken);
            Object phoneClaim = decoded.getClaims().get("phone_number");
            if (phoneClaim == null) {
                log.warn("Firebase token without a verified phone number");
                throw new InvalidCredentialsException("El número no fue verificado");
            }
            return phoneClaim.toString();
        } catch (FirebaseAuthException exception) {
            log.warn("Firebase ID token verification failed: {}", exception.getMessage());
            throw new InvalidCredentialsException("La verificación del número es inválida o expiró");
        }
    }

    private FirebaseApp getFirebaseApp() {
        try {
            return FirebaseApp.getApps().stream()
                    .filter(candidate -> APP_NAME.equals(candidate.getName()))
                    .findFirst()
                    .orElseGet(this::createFirebaseApp);
        } catch (IllegalStateException exception) {
            log.warn("Firebase Auth is unavailable: {}", exception.getMessage());
            throw new InvalidCredentialsException("La verificación del número no está disponible");
        }
    }

    private FirebaseApp createFirebaseApp() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId(projectId)
                    .build();
            return FirebaseApp.initializeApp(options, APP_NAME);
        } catch (IOException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }
}
