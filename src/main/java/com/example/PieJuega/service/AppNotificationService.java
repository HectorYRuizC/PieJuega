package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.AppNotificationResponseDTO;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.model.AppNotification;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.AppNotificationRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppNotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    @Transactional(readOnly = true)
    public List<AppNotificationResponseDTO> getNotifications(Long userId, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_PAGE_SIZE));
        return notificationRepository
                .findByRecipient_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(userId);
    }

    @Transactional
    public AppNotificationResponseDTO markRead(Long notificationId, Long userId) {
        AppNotification notification = notificationRepository
                .findByIdAndRecipient_Id(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    @Transactional
    public AppNotificationResponseDTO notifyUser(
            User recipient,
            NotificationType type,
            String title,
            String body,
            String destination,
            Long sourceId
    ) {
        AppNotification notification = type == NotificationType.CHAT_MESSAGE
                ? notificationRepository
                        .findFirstByRecipient_IdAndTypeAndSourceIdAndReadAtIsNull(
                                recipient.getId(),
                                type,
                                sourceId
                        )
                        .orElseGet(AppNotification::new)
                : new AppNotification();

        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(truncate(title, 120));
        notification.setBody(truncate(body, 500));
        notification.setDestination(destination);
        notification.setSourceId(sourceId);
        notification.setCreatedAt(LocalDateTime.now());

        AppNotificationResponseDTO response = toResponse(
                notificationRepository.save(notification)
        );
        publishAfterCommit(recipient.getId(), response);
        return response;
    }

    @Transactional
    public void notifyAdministrators(
            NotificationType type,
            String title,
            String body,
            String destination,
            Long sourceId
    ) {
        userRepository.findAdministrators().forEach(admin -> notifyUser(
                admin,
                type,
                title,
                body,
                destination,
                sourceId
        ));
    }

    private void publishAfterCommit(Long recipientId, AppNotificationResponseDTO response) {
        Runnable publisher = () -> {
            messagingTemplate.convertAndSend(
                    "/topic/notifications/users/" + recipientId,
                    response
            );
            pushNotificationService.sendToUser(recipientId, response);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publisher.run();
                    }
                }
        );
    }

    private AppNotificationResponseDTO toResponse(AppNotification notification) {
        return new AppNotificationResponseDTO(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getDestination(),
                notification.getSourceId(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }

    private String truncate(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength - 1) + "…";
    }
}
