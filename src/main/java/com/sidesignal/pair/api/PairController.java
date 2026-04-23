package com.sidesignal.pair.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sidesignal.common.security.AuthenticatedUser;
import com.sidesignal.pair.application.PairService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PairController {

    private final PairService pairService;

    // 초대 코드 생성
    @PostMapping("/pair-invites")
    @ResponseStatus(HttpStatus.CREATED)
    public PairInviteResponse createInvite(@AuthenticationPrincipal AuthenticatedUser user) {
        return pairService.createInvite(user.id());
    }

    // 초대 코드 수락 및 페어 생성
    @PostMapping("/pair-invites/{code}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public PairResponse acceptInvite(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String code
    ) {
        return pairService.acceptInvite(user.id(), code);
    }

    // 현재 페어 조회
    @GetMapping("/pairs/current")
    public PairResponse getCurrentPair(@AuthenticationPrincipal AuthenticatedUser user) {
        return pairService.getCurrentPair(user.id());
    }
}
