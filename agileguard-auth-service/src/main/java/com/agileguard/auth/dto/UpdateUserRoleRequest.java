package com.agileguard.auth.dto;

import com.agileguard.common.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Changes a user's role and/or feature team assignment. */
@Data
public class UpdateUserRoleRequest {
    @NotNull(message = "Role is required")
    private Role role;
    private String featureTeamId;   // null = remove from team
}
