package com.sidesignal.signal.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sidesignal.common.security.AuthenticatedUser;
import com.sidesignal.signal.application.SignalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SignalController {

    private final SignalService signalService;

    // 내 상태 조회
    @GetMapping("/me/signal")
    public SignalResponse getMySignal(@AuthenticationPrincipal AuthenticatedUser user) {
        return signalService.getOrCreateMySignal(user.id());
    }

    // 내 상태 업데이트
    @PatchMapping("/me/signal")
    public SignalResponse updateMySignal(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody UpdateSignalRequest request
    ) {
        return signalService.updateMySignal(user.id(), request);
    }

    // 내 퇴근 시간 삭제
    @DeleteMapping("/me/signal/departure-time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDepartureTime(@AuthenticationPrincipal AuthenticatedUser user) {
        signalService.clearDepartureTime(user.id());
    }

    // 페어 상태 목록 조회
    @GetMapping("/pairs/current/signals")
    public PairSignalsResponse getPairSignals(@AuthenticationPrincipal AuthenticatedUser user) {
        return signalService.getPairSignals(user.id());
    }
}
