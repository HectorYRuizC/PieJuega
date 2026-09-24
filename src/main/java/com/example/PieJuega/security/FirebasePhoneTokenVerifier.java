package com.example.PieJuega.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifica ID tokens de Firebase Auth (firmados RS256 por securetoken@system.gserviceaccount.com)
 * sin credenciales de servicio: basta el project ID y los certificados públicos de Google.
 * Se usa para validar server-side que el usuario realmente completó la verificación por SMS
 * antes de permitir operaciones sensibles como el reset de contraseña por teléfono.
 */
@Component
public class FirebasePhoneTokenVerifier {

    private static final String CERTS_URL =
            "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
    private static final Duration CERTS_TTL = Duration.ofHours(1);

    private final String projectId;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, RSAPublicKey> publicKeys = new ConcurrentHashMap<>();
    private volatile Instant keysFetchedAt = Instant.EPOCH;

    public FirebasePhoneTokenVerifier(@Value("${firebase.project-id}") String projectId) {
        this.projectId = projectId;
    }

    /**
     * Valida firma y claims del ID token y devuelve el claim phone_number (formato E.164, ej. +57...).
     * Lanza IllegalArgumentException con mensaje amigable si el token no es válido.
     */
    public String verifyAndGetPhone(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Falta el token de verificación del teléfono");
        }
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(idToken.trim());
        } catch (Exception parseError) {
            throw new IllegalArgumentException("Token de verificación del teléfono inválido");
        }
        if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
            throw new IllegalArgumentException("Token de verificación del teléfono inválido");
        }

        RSAPublicKey key = resolveKey(jwt.getHeader().getKeyID());
        if (key == null) {
            throw new IllegalArgumentException("Token de verificación del teléfono inválido");
        }
        try {
            JWSVerifier verifier = new RSASSAVerifier(key);
            if (!jwt.verify(verifier)) {
                throw new IllegalArgumentException("Token de verificación del teléfono inválido");
            }
        } catch (IllegalArgumentException verificationError) {
            throw verificationError;
        } catch (Exception verificationError) {
            throw new IllegalArgumentException("Token de verificación del teléfono inválido");
        }

        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (Exception claimsError) {
            throw new IllegalArgumentException("Token de verificación del teléfono inválido");
        }

        String issuer = "https://securetoken.google.com/" + projectId;
        if (!issuer.equals(claims.getIssuer())
                || !claims.getAudience().contains(projectId)
                || claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("Token de verificación del teléfono inválido");
        }
        Date expiration = claims.getExpirationTime();
        if (expiration == null || expiration.toInstant().isBefore(Instant.now())) {
            throw new IllegalArgumentException("La verificación del teléfono expiró, inténtalo de nuevo");
        }
        Date issuedAt = claims.getIssueTime();
        if (issuedAt != null && issuedAt.toInstant().isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw new IllegalArgumentException("Token de verificación del teléfono inválido");
        }

        Object phone = claims.getClaim("phone_number");
        if (!(phone instanceof String phoneNumber) || phoneNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "El token no corresponde a una verificación por teléfono");
        }
        return phoneNumber;
    }

    /** Compara dos números de teléfono ignorando formato (+, espacios, guiones). */
    public boolean samePhone(String a, String b) {
        return digitsOnly(a).equals(digitsOnly(b));
    }

    private String digitsOnly(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    private RSAPublicKey resolveKey(String kid) {
        if (kid == null || kid.isBlank()) {
            return null;
        }
        refreshKeysIfNeeded();
        return publicKeys.get(kid);
    }

    private synchronized void refreshKeysIfNeeded() {
        boolean fresh = keysFetchedAt.isAfter(Instant.now().minus(CERTS_TTL));
        if (fresh && !publicKeys.isEmpty()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(CERTS_URL)).GET().build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("No se pudieron obtener las claves públicas de Google");
            }
            Map<String, String> certificates =
                    objectMapper.readValue(response.body(), Map.class);
            Map<String, RSAPublicKey> parsed = new ConcurrentHashMap<>();
            certificates.forEach((keyId, certificatePem) -> {
                RSAPublicKey key = parseCertificateKey((String) certificatePem);
                if (key != null) {
                    parsed.put(keyId, key);
                }
            });
            if (!parsed.isEmpty()) {
                publicKeys.clear();
                publicKeys.putAll(parsed);
                keysFetchedAt = Instant.now();
            }
        } catch (Exception fetchError) {
            if (publicKeys.isEmpty() || keysFetchedAt.isBefore(Instant.now().minus(CERTS_TTL))) {
                throw new IllegalArgumentException(
                        "No se pudo validar la verificación del teléfono, inténtalo de nuevo");
            }
        }
    }

    private RSAPublicKey parseCertificateKey(String certificatePem) {
        try {
            String base64 = certificatePem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            PublicKey publicKey = certificate.getPublicKey();
            if (publicKey instanceof RSAPublicKey rsaKey) {
                return rsaKey;
            }
            // Algunos PEM llegan como SubjectPublicKeyInfo sin encabezado RSA
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKey.getEncoded()));
        } catch (Exception parseError) {
            return null;
        }
    }
}
