package com.example.PieJuega.model;

import com.example.PieJuega.util.PlayerPosition;
import com.example.PieJuega.util.SquadRole;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "team_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_member",
                columnNames = {"team_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_team_members_team", columnList = "team_id"),
                @Index(name = "idx_team_members_user", columnList = "user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private FootballTeam team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "squad_role", nullable = false, length = 16)
    private SquadRole squadRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private PlayerPosition position;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(nullable = false)
    private boolean captain;
}
