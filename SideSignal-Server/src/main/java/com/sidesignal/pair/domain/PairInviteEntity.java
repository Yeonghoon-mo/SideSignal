package com.sidesignal.pair.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.sidesignal.auth.domain.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "pair_invites",
    indexes = {
        @Index(name = "ix_pair_invites_created_by", columnList = "created_by"),
        @Index(name = "ix_pair_invites_expires_at", columnList = "expires_at")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairInviteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code_hash", nullable = false, unique = true, length = 255)
    private String codeHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private UserEntity acceptedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pair_id")
    private PairEntity pair;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PairInviteEntity(String codeHash, UserEntity createdBy, Instant expiresAt) {
        this.codeHash = codeHash;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }

    public void accept(UserEntity acceptedBy, PairEntity pair, Instant acceptedAt) {
        this.acceptedBy = acceptedBy;
        this.pair = pair;
        this.acceptedAt = acceptedAt;
    }
}
