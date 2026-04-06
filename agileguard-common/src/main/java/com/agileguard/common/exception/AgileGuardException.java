package com.agileguard.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all AgileGuard business logic errors.
 * Wraps an HTTP status so controllers can respond correctly.
 */
public class AgileGuardException extends RuntimeException {

    private final HttpStatus status;

    public AgileGuardException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public AgileGuardException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Convenience factory for 404 Not Found. */
    public static AgileGuardException notFound(String resource, String id) {
        return new AgileGuardException(resource + " not found: " + id, HttpStatus.NOT_FOUND);
    }

    /** Convenience factory for 400 Bad Request. */
    public static AgileGuardException badRequest(String message) {
        return new AgileGuardException(message, HttpStatus.BAD_REQUEST);
    }

    /** Convenience factory for 403 Forbidden. */
    public static AgileGuardException forbidden(String message) {
        return new AgileGuardException(message, HttpStatus.FORBIDDEN);
    }

    /** Convenience factory for 409 Conflict. */
    public static AgileGuardException conflict(String message) {
        return new AgileGuardException(message, HttpStatus.CONFLICT);
    }
}
