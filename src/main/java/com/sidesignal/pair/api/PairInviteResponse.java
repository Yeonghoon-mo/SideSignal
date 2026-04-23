package com.sidesignal.pair.api;

import java.time.Instant;

public record PairInviteResponse(
    String inviteCode,
    Instant expiresAt
) {
}
