package com.example.PieJuega.model;

import com.example.PieJuega.util.TeamFormat;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "football_fields",
        indexes = {
                @Index(name = "idx_fields_active_city", columnList = "active, city"),
                @Index(name = "idx_fields_format", columnList = "format")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FootballField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 160)
    private String address;

    @Column(nullable = false, length = 80)
    private String city;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false, length = 600)
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TeamFormat format;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    @CollectionTable(name = "field_features", joinColumns = @JoinColumn(name = "field_id"))
    @Column(name = "feature", nullable = false, length = 80)
    @Builder.Default
    private Set<String> features = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    @CollectionTable(name = "field_open_days", joinColumns = @JoinColumn(name = "field_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 12)
    @Builder.Default
    private Set<DayOfWeek> openDays = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (slotDurationMinutes <= 0) {
            slotDurationMinutes = 60;
        }
    }
}
