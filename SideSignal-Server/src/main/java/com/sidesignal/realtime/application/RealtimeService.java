package com.sidesignal.realtime.application;

import java.io.IOException;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sidesignal.common.error.BusinessException;
import com.sidesignal.common.error.ErrorCode;
import com.sidesignal.pair.domain.PairEntity;
import com.sidesignal.pair.infrastructure.PairRepository;
import com.sidesignal.realtime.infrastructure.SseEmitterRepository;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeService {

    // 기본 타임아웃 1시간
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    private final SseEmitterRepository emitterRepository;
    private final PairRepository pairRepository;

    public SseEmitter subscribe(UUID userId) {
        // 페어 존재 여부 확인 (페어가 없으면 SSE 연결 차단)
        pairRepository.findByFirstUserIdOrSecondUserId(userId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAIR_NOT_FOUND));

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(userId, emitter);

        // 연결 종료, 타임아웃 시 레포지토리에서 제거
        emitter.onCompletion(() -> emitterRepository.deleteById(userId));
        emitter.onTimeout(() -> emitterRepository.deleteById(userId));
        emitter.onError(e -> emitterRepository.deleteById(userId));

        // 연결 직후 더미 이벤트를 보내서 503 에러 방지 및 연결 성립 확인
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (IOException e) {
            emitterRepository.deleteById(userId);
            log.error("SSE 연결 초기화 실패, userId={}", userId, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return emitter;
    }

    // 상태 업데이트 발생 시 상대방에게 이벤트 전송
    public void sendSignalUpdate(SignalUpdatedEventPayload payload) {
        PairEntity pair = pairRepository.findById(payload.pairId())
                .orElse(null);

        if (pair == null) {
            return;
        }

        // 상대방 찾기
        UUID receiverId = pair.getFirstUser().getId().equals(payload.senderId())
                ? pair.getSecondUser().getId()
                : pair.getFirstUser().getId();

        emitterRepository.findById(receiverId).ifPresent(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .id("evt_" + UUID.randomUUID())
                        .name("signal.updated")
                        .data(payload));
            } catch (IOException e) {
                emitterRepository.deleteById(receiverId);
                log.warn("SSE 이벤트 전송 실패 (상대방 연결 끊김 처리), receiverId={}", receiverId, e);
            }
        });
    }

    // 45초 주기로 연결된 모든 클라이언트에게 heartbeat 전송 (Nginx, AWS 등 프록시 타임아웃 방지)
    @Scheduled(fixedRate = 45000)
    public void sendHeartbeat() {
        var emitters = emitterRepository.findAll();
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id("evt_" + UUID.randomUUID())
                        .name("heartbeat")
                        .data(java.util.Map.of("occurredAt", java.time.Instant.now().toString())));
            } catch (IOException e) {
                emitterRepository.deleteById(userId);
                log.warn("SSE 하트비트 전송 실패, 연결 종료. userId={}", userId);
            }
        });
    }
}
