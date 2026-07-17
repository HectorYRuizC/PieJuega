package com.example.PieJuega.repository;

import com.example.PieJuega.model.AppNotification;
import com.example.PieJuega.util.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByRecipient_IdOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable
    );

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);

    Optional<AppNotification> findByIdAndRecipient_Id(Long id, Long recipientId);

    Optional<AppNotification> findFirstByRecipient_IdAndTypeAndSourceIdAndReadAtIsNull(
            Long recipientId,
            NotificationType type,
            Long sourceId
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE AppNotification notification
        SET notification.readAt = :readAt
        WHERE notification.recipient.id = :recipientId
          AND notification.readAt IS NULL
    """)
    int markAllRead(Long recipientId, LocalDateTime readAt);
}
