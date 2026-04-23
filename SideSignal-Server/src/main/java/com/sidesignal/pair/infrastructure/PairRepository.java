package com.sidesignal.pair.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sidesignal.pair.domain.PairEntity;

public interface PairRepository extends JpaRepository<PairEntity, UUID> {

    Optional<PairEntity> findByFirstUserIdOrSecondUserId(UUID firstUserId, UUID secondUserId);

    boolean existsByFirstUserIdOrSecondUserId(UUID firstUserId, UUID secondUserId);
}
