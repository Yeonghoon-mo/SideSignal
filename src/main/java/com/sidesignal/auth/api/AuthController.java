package com.sidesignal.auth.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sidesignal.auth.application.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    // 회원가입과 토큰 발급
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthTokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // 로그인과 토큰 발급
    @PostMapping("/login")
    AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

}
