package com.sidesignal.signal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "시그널", description = "현재 업무 상태 및 퇴근 예정 시간 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SignalController {

    private final SignalService signalService;

    // 내 상태 조회
    @Operation(summary = "내 상태 조회", description = "현재 나의 상태와 퇴근 예정 시간을 조회합니다. 정보가 없으면 기본값으로 생성합니다.")
    @GetMapping("/me/signal")
    public SignalResponse getMySignal(@AuthenticationPrincipal AuthenticatedUser user) {
        return signalService.getOrCreateMySignal(user.id());
    }

    // 내 상태 업데이트
    @Operation(summary = "내 상태 업데이트", description = "나의 현재 상태(ENUM), 퇴근 예정 시간, 짧은 메시지를 업데이트합니다.")
    @PatchMapping("/me/signal")
    public SignalResponse updateMySignal(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody UpdateSignalRequest request
    ) {
        return signalService.updateMySignal(user.id(), request);
    }

    // 내 퇴근 시간 삭제
    @Operation(summary = "내 퇴근 시간 삭제", description = "설정된 퇴근 예정 시간을 초기화(삭제)합니다.")
    @DeleteMapping("/me/signal/departure-time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDepartureTime(@AuthenticationPrincipal AuthenticatedUser user) {
        signalService.clearDepartureTime(user.id());
    }

    // 페어 상태 목록 조회
    @Operation(summary = "페어 멤버 상태 조회", description = "나와 상대방의 최신 상태 정보를 한 번에 조회합니다.")
    @GetMapping("/pairs/current/signals")
    public PairSignalsResponse getPairSignals(@AuthenticationPrincipal AuthenticatedUser user) {
        return signalService.getPairSignals(user.id());
    }
}
