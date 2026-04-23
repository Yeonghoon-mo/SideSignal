package com.sidesignal.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식 오류"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값 검증 실패"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증 필요"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한 없음"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스 없음"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일"),
    ALREADY_PAIRED(HttpStatus.CONFLICT, "ALREADY_PAIRED", "이미 짝이 맺어진 상태"),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "유효하지 않은 초대 코드"),
    INVITE_EXPIRED(HttpStatus.BAD_REQUEST, "INVITE_EXPIRED", "만료된 초대 코드"),
    INVITE_ACCEPTED(HttpStatus.CONFLICT, "INVITE_ACCEPTED", "이미 수락된 초대 코드"),
    CANNOT_ACCEPT_OWN_INVITE(HttpStatus.BAD_REQUEST, "CANNOT_ACCEPT_OWN_INVITE", "자신의 초대 코드는 수락 불가"),
    PAIR_NOT_FOUND(HttpStatus.NOT_FOUND, "PAIR_NOT_FOUND", "페어 정보를 찾을 수 없음"),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청 충돌"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}
