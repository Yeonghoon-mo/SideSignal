package com.sidesignal.signal.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.sidesignal.auth.domain.UserEntity;
import com.sidesignal.pair.domain.PairEntity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "signals",
    uniqueConstraints = @UniqueConstraint(name = "uk_signals_pair_user", columnNames = {"pair_id", "user_id"}),
    indexes = @Index(name = "ix_signals_user_id", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private PairEntity pair;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SignalStatus status = SignalStatus.OFFLINE;

    @Column(name = "departure_time")
    private Instant departureTime;

    @Column(length = 80)
    private String message;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SignalEntity(PairEntity pair, UserEntity user) {
        this.pair = pair;
        this.user = user;
    }

    public void update(SignalStatus status, Instant departureTime, String message) {
        if (status != null) {
            this.status = status;
        }
        this.departureTime = departureTime;
        this.message = message;
    }

    public void clearDepartureTime() {
        this.departureTime = null;
    }
}
