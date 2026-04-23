package com.sidesignal.poke.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sidesignal.common.security.AuthenticatedUser;
import com.sidesignal.poke.application.PokeService;

import lombok.RequiredArgsConstructor;

@Tag(name = "콕 찌르기", description = "페어 상대방에게 가벼운 실시간 알림을 보내는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PokeController {

    private final PokeService pokeService;

    // 상대방 콕 찌르기
    @Operation(summary = "상대방 콕 찌르기", description = "페어 상대방에게 poke.received SSE 이벤트를 전송합니다.")
    @PostMapping("/pokes")
    @ResponseStatus(HttpStatus.CREATED)
    public PokeResponse poke(@AuthenticationPrincipal AuthenticatedUser user) {
        return pokeService.poke(user.id());
    }
}
