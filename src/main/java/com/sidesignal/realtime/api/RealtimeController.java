package com.sidesignal.realtime.api;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sidesignal.common.security.AuthenticatedUser;
import com.sidesignal.realtime.application.RealtimeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RealtimeController {

    private final RealtimeService realtimeService;

    // SSE 이벤트 구독
    @GetMapping(value = "/pairs/current/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthenticatedUser user) {
        return realtimeService.subscribe(user.id());
    }
}
