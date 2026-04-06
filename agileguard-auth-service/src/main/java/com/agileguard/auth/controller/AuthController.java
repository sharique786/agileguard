package com.agileguard.auth.controller;

import com.agileguard.auth.dto.AuthResponse;
import com.agileguard.auth.dto.LoginRequest;
import com.agileguard.auth.dto.RegisterRequest;
import com.agileguard.auth.service.AuthService;
import com.agileguard.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication: register, login, and token refresh.
 * All endpoints are publicly accessible (no auth required).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Registers a new user and returns JWT tokens. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse auth = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", auth));
    }

    /** Authenticates a user and returns JWT tokens. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", auth));
    }

    /** Exchanges a refresh token for a new access token. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {
        AuthResponse auth = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", auth));
    }
}
