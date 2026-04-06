package com.agileguard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Request to create or update a tenant. */
@Data
public class TenantRequest {

    @NotBlank(message = "Tenant name is required")
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
    private String slug;

    private String jiraBaseUrl;
    private String jiraApiToken;
    private String jiraUserEmail;
}
