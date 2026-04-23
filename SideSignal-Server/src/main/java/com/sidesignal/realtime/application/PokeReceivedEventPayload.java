package com.sidesignal.realtime.application;

import java.time.Instant;
import java.util.UUID;

// 콕 찌르기 수신 SSE payload
public record PokeReceivedEventPayload(
    UUID pairId,
    UUID senderId,
    String senderDisplayName,
    Instant occurredAt
) {
}
