package com.sidesignal.signal.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sidesignal.auth.domain.UserEntity;
import com.sidesignal.auth.infrastructure.UserRepository;
import com.sidesignal.common.error.BusinessException;
import com.sidesignal.common.error.ErrorCode;
import com.sidesignal.pair.domain.PairEntity;
import com.sidesignal.pair.infrastructure.PairRepository;
import com.sidesignal.realtime.application.SignalUpdatedEventPayload;
import com.sidesignal.signal.api.PairSignalsResponse;
import com.sidesignal.signal.api.SignalResponse;
import com.sidesignal.signal.api.UpdateSignalRequest;
import com.sidesignal.signal.domain.SignalEntity;
import com.sidesignal.signal.domain.SignalEventEntity;
import com.sidesignal.signal.domain.SignalEventType;
import com.sidesignal.signal.infrastructure.SignalEventRepository;
import com.sidesignal.signal.infrastructure.SignalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalService {

    private final SignalRepository signalRepository;
    private final SignalEventRepository signalEventRepository;
    private final PairRepository pairRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 내 상태 조회 (없으면 생성해서 반환)
    @Transactional
    public SignalResponse getOrCreateMySignal(UUID userId) {
        PairEntity pair = getMyPair(userId);
        SignalEntity signal = getOrCreateSignalEntity(pair, userId);
        return SignalResponse.from(signal);
    }

    // 내 상태 업데이트
    @Transactional
    public SignalResponse updateMySignal(UUID userId, UpdateSignalRequest request) {
        PairEntity pair = getMyPair(userId);
        SignalEntity signal = getOrCreateSignalEntity(pair, userId);

        signal.update(request.status(), request.departureTime(), request.message());
        
        // 이벤트 기록
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("status", request.status().name());
        if (request.departureTime() != null) {
            payload.put("departureTime", request.departureTime().toString());
        }
        if (request.message() != null) {
            payload.put("message", request.message());
        }

        recordSignalEvent(pair, signal, SignalEventType.SIGNAL_UPDATED, payload);

        log.info("signal_updated userId={}, pairId={}, status={}", userId, pair.getId(), request.status());

        return SignalResponse.from(signal);
    }

    // 퇴근 시간 지우기
    @Transactional
    public void clearDepartureTime(UUID userId) {
        PairEntity pair = getMyPair(userId);
        SignalEntity signal = getOrCreateSignalEntity(pair, userId);

        signal.clearDepartureTime();
        
        // 이벤트 기록
        recordSignalEvent(pair, signal, SignalEventType.DEPARTURE_TIME_CLEARED, Map.of());

        log.info("departure_time_cleared userId={}, pairId={}", userId, pair.getId());
    }

    // 페어 멤버들의 최신 상태 조회
    @Transactional
    public PairSignalsResponse getPairSignals(UUID userId) {
        PairEntity pair = getMyPair(userId);
        
        // Ensure both signals exist in the db for consistent pair view
        getOrCreateSignalEntity(pair, pair.getFirstUser().getId());
        getOrCreateSignalEntity(pair, pair.getSecondUser().getId());

        List<SignalResponse> signals = signalRepository.findAllByPairId(pair.getId())
            .stream()
            .map(SignalResponse::from)
            .toList();

        return PairSignalsResponse.from(signals);
    }

    private PairEntity getMyPair(UUID userId) {
        return pairRepository.findByFirstUserIdOrSecondUserId(userId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAIR_NOT_FOUND));
    }

    private SignalEntity getOrCreateSignalEntity(PairEntity pair, UUID targetUserId) {
        return signalRepository.findByPairIdAndUserId(pair.getId(), targetUserId)
            .orElseGet(() -> {
                UserEntity user = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
                SignalEntity newSignal = new SignalEntity(pair, user);
                return signalRepository.save(newSignal);
            });
    }

    private void recordSignalEvent(PairEntity pair, SignalEntity signal, SignalEventType type, Map<String, Object> payload) {
        SignalEventEntity event = new SignalEventEntity(pair, signal.getUser(), type, payload);
        signalEventRepository.save(event);

        eventPublisher.publishEvent(new SignalUpdatedEventPayload(
            pair.getId(),
            signal.getUser().getId(),
            signal.getStatus().name(),
            signal.getDepartureTime(),
            Instant.now()
        ));
    }
}
