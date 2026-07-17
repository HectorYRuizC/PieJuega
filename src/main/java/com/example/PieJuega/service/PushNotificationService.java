package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.AppNotificationResponseDTO;
import com.example.PieJuega.model.PushDevice;
import com.example.PieJuega.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private static final int FCM_BATCH_SIZE = 500;

    private final PushDeviceRepository pushDeviceRepository;
    private final FirebaseMessagingGateway firebaseMessagingGateway;

    @Async("pushNotificationExecutor")
    @Transactional
    public void sendToUser(
            Long userId,
            AppNotificationResponseDTO notification
    ) {
        Map<DeliveryPreferences, List<String>> deliveries = pushDeviceRepository
                .findByUser_Id(userId)
                .stream()
                .collect(Collectors.groupingBy(
                        device -> new DeliveryPreferences(
                                device.isSoundEnabled(),
                                device.isVibrationEnabled()
                        ),
                        Collectors.mapping(PushDevice::getToken, Collectors.toList())
                ));

        for (Map.Entry<DeliveryPreferences, List<String>> delivery
                : deliveries.entrySet()) {
            List<String> tokens = delivery.getValue();
            DeliveryPreferences preferences = delivery.getKey();
            for (int start = 0; start < tokens.size(); start += FCM_BATCH_SIZE) {
                int end = Math.min(start + FCM_BATCH_SIZE, tokens.size());
                Set<String> invalidTokens = firebaseMessagingGateway.send(
                        tokens.subList(start, end),
                        notification,
                        preferences.soundEnabled(),
                        preferences.vibrationEnabled()
                );
                if (!invalidTokens.isEmpty()) {
                    pushDeviceRepository.deleteByTokenIn(invalidTokens);
                }
            }
        }
    }

    private record DeliveryPreferences(
            boolean soundEnabled,
            boolean vibrationEnabled
    ) {
    }
}
