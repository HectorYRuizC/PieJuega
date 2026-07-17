package com.example.PieJuega.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_room_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room_member",
                columnNames = {"room_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_chat_members_user", columnList = "user_id"),
                @Index(name = "idx_chat_members_room", columnList = "room_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Builder.Default
    @Column(name = "last_read_message_id", nullable = false)
    private Long lastReadMessageId = 0L;

    @PrePersist
    void onCreate() {
        joinedAt = LocalDateTime.now();
        if (lastReadMessageId == null) {
            lastReadMessageId = 0L;
        }
    }
}
