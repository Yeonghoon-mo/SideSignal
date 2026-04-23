package com.sidesignal.pair.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sidesignal.pair.domain.PairInviteEntity;

public interface PairInviteRepository extends JpaRepository<PairInviteEntity, UUID> {

    Optional<PairInviteEntity> findByCodeHash(String codeHash);
}
