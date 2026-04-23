package com.sidesignal.signal.api;

import java.time.Instant;
import java.util.UUID;

import com.sidesignal.signal.domain.SignalEntity;
import com.sidesignal.signal.domain.SignalStatus;

public record SignalResponse(
    UUID id,
    UUID userId,
    SignalStatus status,
    Instant departureTime,
    String message,
    Instant updatedAt
) {
    public static SignalResponse from(SignalEntity signal) {
        return new SignalResponse(
            signal.getId(),
            signal.getUser().getId(),
            signal.getStatus(),
            signal.getDepartureTime(),
            signal.getMessage(),
            signal.getUpdatedAt()
        );
    }
}
