package com.sidesignal.poke.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sidesignal.auth.domain.UserEntity;
import com.sidesignal.common.error.BusinessException;
import com.sidesignal.common.error.ErrorCode;
import com.sidesignal.pair.domain.PairEntity;
import com.sidesignal.pair.infrastructure.PairRepository;
import com.sidesignal.poke.api.PokeResponse;
import com.sidesignal.realtime.application.PokeReceivedEventPayload;
import com.sidesignal.signal.domain.SignalEventEntity;
import com.sidesignal.signal.domain.SignalEventType;
import com.sidesignal.signal.infrastructure.SignalEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PokeService {

    // 콕 찌르기 반복 제한 시간
    private static final Duration POKE_COOLDOWN = Duration.ofSeconds(10);

    private final PairRepository pairRepository;
    private final SignalEventRepository signalEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // 상대방 콕 찌르기
    @Transactional
    public PokeResponse poke(UUID senderId) {
        PairEntity pair = pairRepository.findByFirstUserIdOrSecondUserId(senderId, senderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAIR_NOT_FOUND));
        UserEntity sender = findSender(pair, senderId);
        UserEntity recipient = findRecipient(pair, senderId);
        Instant now = clock.instant();

        validateCooldown(pair.getId(), senderId, now);

        signalEventRepository.save(new SignalEventEntity(
            pair,
            sender,
            SignalEventType.POKE_SENT,
            Map.of("recipientId", recipient.getId().toString())
        ));
        eventPublisher.publishEvent(new PokeReceivedEventPayload(
            pair.getId(),
            sender.getId(),
            sender.getDisplayName(),
            now
        ));

        log.info("poke_sent senderId={}, recipientId={}, pairId={}", sender.getId(), recipient.getId(), pair.getId());

        return new PokeResponse(recipient.getDisplayName(), now);
    }

    private void validateCooldown(UUID pairId, UUID senderId, Instant now) {
        boolean exists = signalEventRepository.existsByPairIdAndSenderIdAndEventTypeAndCreatedAtAfter(
            pairId,
            senderId,
            SignalEventType.POKE_SENT,
            now.minus(POKE_COOLDOWN)
        );

        if (exists) {
            throw new BusinessException(ErrorCode.POKE_COOLDOWN);
        }
    }

    private UserEntity findSender(PairEntity pair, UUID senderId) {
        if (pair.getFirstUser().getId().equals(senderId)) {
            return pair.getFirstUser();
        }

        return pair.getSecondUser();
    }

    private UserEntity findRecipient(PairEntity pair, UUID senderId) {
        if (pair.getFirstUser().getId().equals(senderId)) {
            return pair.getSecondUser();
        }

        return pair.getFirstUser();
    }
}
