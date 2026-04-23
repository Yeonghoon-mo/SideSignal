package com.sidesignal.common.security;

public record TokenIssueResult(
    String accessToken,
    long expiresIn
) {
}
