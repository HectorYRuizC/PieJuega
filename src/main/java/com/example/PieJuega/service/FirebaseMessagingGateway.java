package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.AppNotificationResponseDTO;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class FirebaseMessagingGateway {

    private static final String APP_NAME = "piejuega-push";

    private final boolean enabled;
    private final String projectId;
    private FirebaseMessaging messaging;

    public FirebaseMessagingGateway(
            @Value("${firebase.messaging.enabled:true}") boolean enabled,
            @Value("${firebase.project-id}") String projectId
    ) {
        this.enabled = enabled;
        this.projectId = projectId;
    }

    @PostConstruct
    void initialize() {
        if (!enabled) {
            log.info("Firebase Cloud Messaging is disabled by configuration");
            return;
        }

        try {
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(candidate -> APP_NAME.equals(candidate.getName()))
                    .findFirst()
                    .orElseGet(this::createFirebaseApp);
            messaging = FirebaseMessaging.getInstance(app);
            log.info("Firebase Cloud Messaging initialized for project {}", projectId);
        } catch (RuntimeException exception) {
            log.warn(
                    "Firebase Cloud Messaging is unavailable. Configure "
                            + "GOOGLE_APPLICATION_CREDENTIALS to enable remote push notifications: {}",
                    exception.getMessage()
            );
        }
    }

    public Set<String> send(
            List<String> tokens,
            AppNotificationResponseDTO notification,
            boolean soundEnabled,
            boolean vibrationEnabled
    ) {
        if (messaging == null || tokens.isEmpty()) {
            return Set.of();
        }

        Aps.Builder aps = Aps.builder();
        AndroidNotification.Builder androidNotification =
                AndroidNotification.builder()
                        .setDefaultVibrateTimings(vibrationEnabled);
        if (soundEnabled) {
            aps.setSound("default");
            androidNotification.setSound("default");
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(notification.title())
                        .setBody(notification.body())
                        .build())
                .putData("notificationId", String.valueOf(notification.id()))
                .putData("type", notification.type().name())
                .putData(
                        "destination",
                        notification.destination() == null ? "" : notification.destination()
                )
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(androidNotification.build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", "10")
                        .setAps(aps.build())
                        .build())
                .build();

        try {
            BatchResponse response = messaging.sendEachForMulticast(message);
            return invalidTokens(tokens, response.getResponses());
        } catch (FirebaseMessagingException exception) {
            log.warn("Could not deliver Firebase push notification: {}", exception.getMessage());
            return Set.of();
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

    private Set<String> invalidTokens(
            List<String> tokens,
            List<SendResponse> responses
    ) {
        Set<String> invalid = new HashSet<>();
        for (int index = 0; index < responses.size(); index++) {
            SendResponse response = responses.get(index);
            if (response.isSuccessful()) {
                continue;
            }
            MessagingErrorCode errorCode = response.getException().getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED
                    || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                invalid.add(tokens.get(index));
            }
        }
        return invalid;
    }
}
