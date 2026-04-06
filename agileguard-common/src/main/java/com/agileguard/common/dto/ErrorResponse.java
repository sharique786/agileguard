package com.agileguard.common.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Structured error response returned by all AgileGuard services
 * when an exception occurs. Includes field-level validation errors.
 */
@Data
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldError> fieldErrors;

    @Builder.Default
    private Instant timestamp = Instant.now();

    /** Represents a validation error on a specific field. */
    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
