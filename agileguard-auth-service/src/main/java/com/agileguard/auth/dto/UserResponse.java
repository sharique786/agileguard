package com.agileguard.auth.dto;

import com.agileguard.common.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Safe user projection returned from list/get endpoints.
 * Never exposes the hashed password.
 */
@Data
@Builder
public class UserResponse {
    private String id;
    private String email;
    private String fullName;
    private Role role;
    private String tenantId;
    private String tenantName;
    private String featureTeamId;
    private String featureTeamName;
    private String projectTeamId;
    private String projectTeamName;
    private String githubUsername;
    private String jiraAccountId;
    private boolean active;
    private Instant createdAt;
}
