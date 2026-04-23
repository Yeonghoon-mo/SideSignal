package com.sidesignal.pair.api;

import java.time.Instant;
import java.util.UUID;

import com.sidesignal.auth.api.AuthUserResponse;
import com.sidesignal.pair.domain.PairEntity;

public record PairResponse(
    UUID id,
    AuthUserResponse firstUser,
    AuthUserResponse secondUser,
    Instant createdAt
) {
    public static PairResponse from(PairEntity pair) {
        return new PairResponse(
            pair.getId(),
            AuthUserResponse.from(pair.getFirstUser()),
            AuthUserResponse.from(pair.getSecondUser()),
            pair.getCreatedAt()
        );
    }
}
