package com.agileguard.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Partial update request for an existing tenant.
 * Null fields are ignored (patch semantics).
 */
@Data
public class UpdateTenantRequest {
    @Size(min = 2, max = 100)
    private String name;
    private String jiraBaseUrl;
    private String jiraUserEmail;
    private String jiraApiToken;
    private String plan;
    private Boolean active;
}
