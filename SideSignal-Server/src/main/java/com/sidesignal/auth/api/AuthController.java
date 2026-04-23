package com.sidesignal.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sidesignal.auth.application.AuthService;

@Tag(name = "인증", description = "회원가입 및 로그인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    // 회원가입과 토큰 발급
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록하고 JWT 토큰을 발급합니다.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthTokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // 로그인과 토큰 발급
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급합니다.")
    @PostMapping("/login")
    AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

}
