package com.sidesignal.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    List<FieldErrorResponse> fieldErrors
) {

    // 기본 에러 응답 생성
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return of(errorCode, errorCode.getMessage(), path);
    }

    // 사용자 지정 메시지 에러 응답 생성
    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
            Instant.now(),
            errorCode.getHttpStatus().value(),
            errorCode.getCode(),
            message,
            path,
            List.of()
        );
    }

    // 필드 검증 실패 응답 생성
    public static ErrorResponse validation(List<FieldErrorResponse> fieldErrors, String path) {
        return new ErrorResponse(
            Instant.now(),
            ErrorCode.VALIDATION_ERROR.getHttpStatus().value(),
            ErrorCode.VALIDATION_ERROR.getCode(),
            ErrorCode.VALIDATION_ERROR.getMessage(),
            path,
            fieldErrors
        );
    }

}
