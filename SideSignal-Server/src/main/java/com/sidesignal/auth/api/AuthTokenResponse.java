package com.sidesignal.auth.api;

public record AuthTokenResponse(
    String tokenType,
    String accessToken,
    long expiresIn,
    AuthUserResponse user
) {
}
