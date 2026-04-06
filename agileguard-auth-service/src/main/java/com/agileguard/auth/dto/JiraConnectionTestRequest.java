package com.agileguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Tests a JIRA connection before persisting credentials. */
@Data
public class JiraConnectionTestRequest {
    @NotBlank(message = "JIRA base URL is required")
    private String baseUrl;

    @NotBlank @Email
    private String userEmail;

    @NotBlank(message = "JIRA API token is required")
    private String apiToken;
}
