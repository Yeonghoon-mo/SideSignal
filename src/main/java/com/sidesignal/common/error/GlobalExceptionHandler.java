package com.sidesignal.common.error;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 비즈니스 예외 응답 변환
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode, exception.getMessage(), request.getRequestURI()));
    }

    // 인증 예외 응답 변환
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthenticationException(HttpServletRequest request) {
        return ResponseEntity
            .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
            .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, request.getRequestURI()));
    }

    // 인가 예외 응답 변환
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDeniedException(HttpServletRequest request) {
        return ResponseEntity
            .status(ErrorCode.FORBIDDEN.getHttpStatus())
            .body(ErrorResponse.of(ErrorCode.FORBIDDEN, request.getRequestURI()));
    }

    // 라우팅 실패 응답 변환
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ErrorResponse> handleNotFoundException(HttpServletRequest request) {
        return ResponseEntity
            .status(ErrorCode.NOT_FOUND.getHttpStatus())
            .body(ErrorResponse.of(ErrorCode.NOT_FOUND, request.getRequestURI()));
    }

    // RequestBody 검증 실패 응답 변환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors()
            .stream()
            .map(fieldError -> new FieldErrorResponse(
                fieldError.getField(),
                fieldErrorMessage(fieldError.getDefaultMessage())
            ))
            .toList();

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
            .body(ErrorResponse.validation(fieldErrors, request.getRequestURI()));
    }

    // RequestParam과 PathVariable 검증 실패 응답 변환
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
            .stream()
            .map(violation -> new FieldErrorResponse(
                violation.getPropertyPath().toString(),
                violation.getMessage()
            ))
            .toList();

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
            .body(ErrorResponse.validation(fieldErrors, request.getRequestURI()));
    }

    // JSON 파싱 실패 응답 변환
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpServletRequest request) {
        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
            .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getRequestURI()));
    }

    // 예상 외 서버 오류 응답 변환
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("unexpected_exception path={}", request.getRequestURI(), exception);

        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
            .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }

    // 빈 검증 메시지 기본값
    private static String fieldErrorMessage(String message) {
        if (StringUtils.hasText(message)) {
            return message;
        }

        return ErrorCode.VALIDATION_ERROR.getMessage();
    }

}
