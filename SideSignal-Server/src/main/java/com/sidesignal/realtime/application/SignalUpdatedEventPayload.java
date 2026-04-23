package com.sidesignal.realtime.application;

import java.time.Instant;
import java.util.UUID;

public record SignalUpdatedEventPayload(
    UUID pairId,
    UUID senderId,
    String status,
    Instant departureTime,
    Instant occurredAt
) {
}
