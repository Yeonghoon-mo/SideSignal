package com.sidesignal.common.error;

public record FieldErrorResponse(
    String field,
    String message
) {
}
