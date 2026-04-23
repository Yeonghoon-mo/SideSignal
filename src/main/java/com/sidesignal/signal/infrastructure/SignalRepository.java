package com.sidesignal.signal.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sidesignal.signal.domain.SignalEntity;

public interface SignalRepository extends JpaRepository<SignalEntity, UUID> {

    Optional<SignalEntity> findByPairIdAndUserId(UUID pairId, UUID userId);

    List<SignalEntity> findAllByPairId(UUID pairId);
}
