package com.sidesignal.pair.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sidesignal.auth.domain.UserEntity;
import com.sidesignal.auth.infrastructure.UserRepository;
import com.sidesignal.common.error.BusinessException;
import com.sidesignal.common.error.ErrorCode;
import com.sidesignal.common.security.HashUtils;
import com.sidesignal.pair.api.PairInviteResponse;
import com.sidesignal.pair.api.PairResponse;
import com.sidesignal.pair.domain.PairEntity;
import com.sidesignal.pair.domain.PairInviteEntity;
import com.sidesignal.pair.infrastructure.PairInviteRepository;
import com.sidesignal.pair.infrastructure.PairRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PairService {

    private final PairRepository pairRepository;
    private final PairInviteRepository pairInviteRepository;
    private final UserRepository userRepository;
    private final Clock clock = Clock.systemUTC();

    // 초대 코드 생성 (유효기간 24시간)
    @Transactional
    public PairInviteResponse createInvite(UUID userId) {
        if (pairRepository.existsByFirstUserIdOrSecondUserId(userId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_PAIRED);
        }

        // 사용자 존재 여부 확인
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        String inviteCode = UUID.randomUUID().toString().split("-")[0].toUpperCase();
        String codeHash = HashUtils.sha256(inviteCode);
        Instant expiresAt = Instant.now(clock).plus(24, ChronoUnit.HOURS);

        PairInviteEntity invite = new PairInviteEntity(codeHash, user, expiresAt);
        pairInviteRepository.save(invite);

        log.info("pair_invite_created userId={}", userId);

        return new PairInviteResponse(inviteCode, expiresAt);
    }

    // 초대 코드 수락 및 페어 생성
    @Transactional
    public PairResponse acceptInvite(UUID userId, String inviteCode) {
        if (pairRepository.existsByFirstUserIdOrSecondUserId(userId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_PAIRED);
        }

        String codeHash = HashUtils.sha256(inviteCode);
        PairInviteEntity invite = pairInviteRepository.findByCodeHash(codeHash)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));

        Instant now = Instant.now(clock);
        if (invite.getExpiresAt().isBefore(now)) {
            throw new BusinessException(ErrorCode.INVITE_EXPIRED);
        }
        if (invite.getAcceptedAt() != null) {
            throw new BusinessException(ErrorCode.INVITE_ACCEPTED);
        }
        if (invite.getCreatedBy().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_ACCEPT_OWN_INVITE);
        }

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        PairEntity pair = new PairEntity(invite.getCreatedBy(), user);
        pairRepository.save(pair);

        invite.accept(user, pair, now);

        log.info("pair_invite_accepted userId={}, pairId={}, inviteCreatedBy={}", 
            userId, pair.getId(), invite.getCreatedBy().getId());

        return PairResponse.from(pair);
    }

    // 현재 접속자의 페어 조회
    @Transactional(readOnly = true)
    public PairResponse getCurrentPair(UUID userId) {
        PairEntity pair = pairRepository.findByFirstUserIdOrSecondUserId(userId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAIR_NOT_FOUND));

        return PairResponse.from(pair);
    }
}
