package com.sidesignal.auth.api;

import java.util.UUID;

import com.sidesignal.auth.domain.UserEntity;

public record AuthUserResponse(
    UUID id,
    String email,
    String displayName
) {

    // 사용자 엔티티 응답 변환
    public static AuthUserResponse from(UserEntity user) {
        return new AuthUserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName()
        );
    }

}
