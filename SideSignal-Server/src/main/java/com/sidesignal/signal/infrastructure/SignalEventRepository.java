package com.sidesignal.signal.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sidesignal.signal.domain.SignalEventType;
import com.sidesignal.signal.domain.SignalEventEntity;

public interface SignalEventRepository extends JpaRepository<SignalEventEntity, UUID> {

    List<SignalEventEntity> findTop100ByPairIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID pairId, Instant createdAt);

    // 콕 찌르기 쿨다운 중복 이벤트 확인
    boolean existsByPairIdAndSenderIdAndEventTypeAndCreatedAtAfter(
        UUID pairId,
        UUID senderId,
        SignalEventType eventType,
        Instant createdAt
    );
}
