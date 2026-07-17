package com.example.PieJuega.model;

import com.example.PieJuega.util.TeamFormat;
import com.example.PieJuega.util.TournamentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tournaments",
        indexes = {
                @Index(name = "idx_tournaments_status_start", columnList = "status, starts_at"),
                @Index(name = "idx_tournaments_creator", columnList = "creator_id, created_at"),
                @Index(name = "idx_tournaments_field", columnList = "field_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 800)
    private String description;

    @Column(length = 1200)
    private String rules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TeamFormat format;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private FootballField field;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(name = "max_teams", nullable = false)
    private int maxTeams;

    @Column(name = "entry_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal entryFee;

    @Column(length = 200)
    private String prize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TournamentStatus status;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) {
            status = TournamentStatus.PENDING_APPROVAL;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
