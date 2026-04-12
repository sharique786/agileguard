package com.agileguard.jira.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload to create a new JIRA Epic via AgileGuard.
 *
 * Sent from the Story Editor "Create New Epic" form.
 * Maps to the JIRA REST API v3 /rest/api/3/issue endpoint
 * with issuetype.name = "Epic".
 *
 * JIRA field mapping:
 *   epicName     → fields.summary (and customfield_10011 in classic projects)
 *   description  → fields.description (ADF in real calls)
 *   priority     → fields.priority.name
 *   businessLine → fields.customfield_10100 (example custom field)
 */
@Data
public class CreateEpicRequest {

    @NotBlank(message = "Project key is required")
    private String projectKey;

    @NotBlank(message = "Epic name is required")
    @Size(min = 5, max = 255, message = "Epic name must be 5–255 characters")
    private String epicName;

    @Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    /** Blocker | Critical | Major | Medium | Minor | Low | Trivial */
    private String priority;

    /** Business | GT | Platform | Infrastructure | Data | Security */
    private String businessLine;

    private String reporterAccountId;
    private String reporterName;
}
