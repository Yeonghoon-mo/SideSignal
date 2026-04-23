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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "pairs",
    indexes = {
        @Index(name = "ix_pairs_first_user_id", columnList = "first_user_id"),
        @Index(name = "ix_pairs_second_user_id", columnList = "second_user_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "first_user_id", nullable = false)
    private UserEntity firstUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "second_user_id", nullable = false)
    private UserEntity secondUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PairEntity(UserEntity firstUser, UserEntity secondUser) {
        this.firstUser = firstUser;
        this.secondUser = secondUser;
    }
}
