package com.agileguard.auth.dto;

import com.agileguard.common.enums.Role;
import lombok.Builder;
import lombok.Data;

/** JWT tokens and user info returned after successful authentication. */
@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String userId;
    private String email;
    private String fullName;
    private Role role;
    private String tenantId;
    private String featureTeamId;
}
