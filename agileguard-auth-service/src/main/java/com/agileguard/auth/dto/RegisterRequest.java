package com.agileguard.auth.dto;

import com.agileguard.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Registration request for a new user. */
@Data
public class RegisterRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    private Role role = Role.DEVELOPER;
    private String featureTeamId;
    private String githubUsername;
    private String jiraAccountId;
}
