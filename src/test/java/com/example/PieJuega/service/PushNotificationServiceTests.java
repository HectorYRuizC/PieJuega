package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.AppNotificationResponseDTO;
import com.example.PieJuega.model.PushDevice;
import com.example.PieJuega.repository.PushDeviceRepository;
import com.example.PieJuega.util.DevicePlatform;
import com.example.PieJuega.util.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTests {

    @Mock
    private PushDeviceRepository pushDeviceRepository;

    @Mock
    private FirebaseMessagingGateway firebaseMessagingGateway;

    @Test
    void groupsDeliveriesByPreferencesAndRemovesInvalidTokens() {
        AppNotificationResponseDTO notification = new AppNotificationResponseDTO(
                3L,
                NotificationType.TEAM_ADDED,
                "Nuevo equipo",
                "Te agregaron al equipo",
                "/createTeam",
                9L,
                false,
                LocalDateTime.now()
        );
        PushDevice audible = device("audible", true, true);
        PushDevice silent = device("silent", false, true);
        when(pushDeviceRepository.findByUser_Id(7L))
                .thenReturn(List.of(audible, silent));
        doReturn(Set.of()).when(firebaseMessagingGateway).send(
                argThat(tokens -> tokens.contains("audible")),
                eq(notification),
                eq(true),
                eq(true)
        );
        doReturn(Set.of("silent")).when(firebaseMessagingGateway).send(
                argThat(tokens -> tokens.contains("silent")),
                eq(notification),
                eq(false),
                eq(true)
        );
        PushNotificationService service = new PushNotificationService(
                pushDeviceRepository,
                firebaseMessagingGateway
        );

        service.sendToUser(7L, notification);

        verify(firebaseMessagingGateway).send(
                argThat(tokens -> tokens.equals(List.of("audible"))),
                eq(notification),
                eq(true),
                eq(true)
        );
        verify(firebaseMessagingGateway).send(
                argThat(tokens -> tokens.equals(List.of("silent"))),
                eq(notification),
                eq(false),
                eq(true)
        );
        verify(pushDeviceRepository).deleteByTokenIn(Set.of("silent"));
    }

    private PushDevice device(
            String token,
            boolean soundEnabled,
            boolean vibrationEnabled
    ) {
        return PushDevice.builder()
                .token(token)
                .platform(DevicePlatform.IOS)
                .soundEnabled(soundEnabled)
                .vibrationEnabled(vibrationEnabled)
                .build();
    }
}
