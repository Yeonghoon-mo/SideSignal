package com.sidesignal.signal.api;

import java.time.Instant;

import com.sidesignal.signal.domain.SignalStatus;

import jakarta.validation.constraints.Size;

public record UpdateSignalRequest(
    SignalStatus status,

    Instant departureTime,

    @Size(max = 80, message = "메시지는 80자 이내여야 합니다")
    String message
) {
}
