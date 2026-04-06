package com.agileguard.jira.config;

import com.agileguard.common.dto.ErrorResponse;
import com.agileguard.common.exception.AgileGuardException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AgileGuardException.class)
    public ResponseEntity<ErrorResponse> handleAgileGuardException(
            AgileGuardException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.builder()
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception:", ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.builder()
                .status(500).error("Internal Server Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }
}
