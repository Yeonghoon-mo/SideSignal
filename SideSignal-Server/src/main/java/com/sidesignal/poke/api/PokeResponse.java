package com.sidesignal.poke.api;

import java.time.Instant;

// 콕 찌르기 요청 응답
public record PokeResponse(
    String recipientDisplayName,
    Instant sentAt
) {
}
