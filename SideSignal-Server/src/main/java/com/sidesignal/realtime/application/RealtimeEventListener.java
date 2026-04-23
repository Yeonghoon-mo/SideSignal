package com.sidesignal.realtime.application;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RealtimeEventListener {

    private final RealtimeService realtimeService;

    @Async
    @EventListener
    public void handleSignalUpdatedEvent(SignalUpdatedEventPayload payload) {
        realtimeService.sendSignalUpdate(payload);
    }

    // 콕 찌르기 이벤트 비동기 전송
    @Async
    @EventListener
    public void handlePokeReceivedEvent(PokeReceivedEventPayload payload) {
        realtimeService.sendPokeReceived(payload);
    }
}
