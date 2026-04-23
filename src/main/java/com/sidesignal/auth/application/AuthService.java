package com.sidesignal.auth.application;

import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sidesignal.auth.api.AuthTokenResponse;
import com.sidesignal.auth.api.AuthUserResponse;
import com.sidesignal.auth.api.LoginRequest;
import com.sidesignal.auth.api.RegisterRequest;
import com.sidesignal.auth.domain.UserEntity;
import com.sidesignal.auth.infrastructure.UserRepository;
import com.sidesignal.common.error.BusinessException;
import com.sidesignal.common.error.ErrorCode;
import com.sidesignal.common.security.JwtTokenProvider;
import com.sidesignal.common.security.TokenIssueResult;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 신규 사용자 생성과 access token 발급
    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        // 이메일 중복 방지
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 해시 저장
        UserEntity user = userRepository.save(new UserEntity(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        ));
        TokenIssueResult token = jwtTokenProvider.issue(user);

        return toTokenResponse(token, user);
    }

    // 사용자 인증과 access token 재발급
    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호 오류"));

        // 비밀번호 불일치 차단
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호 오류");
        }

        TokenIssueResult token = jwtTokenProvider.issue(user);

        return toTokenResponse(token, user);
    }

    // 인증 응답 조립
    private AuthTokenResponse toTokenResponse(TokenIssueResult token, UserEntity user) {
        return new AuthTokenResponse(
                BEARER_TOKEN_TYPE,
                token.accessToken(),
                token.expiresIn(),
                AuthUserResponse.from(user)
        );
    }

    // 이메일 비교 기준 정규화
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
