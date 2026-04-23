package com.sidesignal.pair.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "페어링", description = "초대 코드 생성 및 수락을 통한 사용자 연결 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PairController {

    private final PairService pairService;

    // 초대 코드 생성
    @Operation(summary = "초대 코드 생성", description = "상대방을 초대하기 위한 8자리 영숫자 코드를 생성합니다. 24시간 동안 유효합니다.")
    @PostMapping("/pair-invites")
    @ResponseStatus(HttpStatus.CREATED)
    public PairInviteResponse createInvite(@AuthenticationPrincipal AuthenticatedUser user) {
        return pairService.createInvite(user.id());
    }

    // 초대 코드 수락 및 페어 생성
    @Operation(summary = "초대 코드 수락", description = "상대방이 생성한 초대 코드를 입력하여 페어(Pair) 관계를 맺습니다.")
    @PostMapping("/pair-invites/{code}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public PairResponse acceptInvite(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String code
    ) {
        return pairService.acceptInvite(user.id(), code);
    }

    // 현재 페어 조회
    @Operation(summary = "현재 페어 정보 조회", description = "현재 접속한 사용자가 속한 페어 정보를 조회합니다.")
    @GetMapping("/pairs/current")
    public PairResponse getCurrentPair(@AuthenticationPrincipal AuthenticatedUser user) {
        return pairService.getCurrentPair(user.id());
    }
}
