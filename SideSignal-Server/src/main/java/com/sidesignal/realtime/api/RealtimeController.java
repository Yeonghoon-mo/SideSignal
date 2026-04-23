package com.sidesignal.realtime.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sidesignal.common.security.AuthenticatedUser;
import com.sidesignal.realtime.application.RealtimeService;

import lombok.RequiredArgsConstructor;

@Tag(name = "실시간 통신", description = "SSE(Server-Sent Events)를 이용한 실시간 이벤트 스트림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RealtimeController {

    private final RealtimeService realtimeService;

    // SSE 이벤트 구독
    @Operation(summary = "실시간 이벤트 스트림 구독", description = "SSE 연결을 통해 상대방의 상태 변경 알림(signal.updated)을 실시간으로 수신합니다.")
    @GetMapping(value = "/pairs/current/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthenticatedUser user) {
        return realtimeService.subscribe(user.id());
    }
}
