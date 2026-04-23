package com.sidesignal.signal.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sidesignal.signal.domain.SignalEventEntity;

public interface SignalEventRepository extends JpaRepository<SignalEventEntity, UUID> {

    List<SignalEventEntity> findTop100ByPairIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID pairId, Instant createdAt);
}
