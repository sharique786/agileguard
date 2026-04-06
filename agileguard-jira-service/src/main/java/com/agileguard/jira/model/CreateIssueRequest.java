package com.agileguard.jira.model;

import com.agileguard.common.enums.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Full payload for creating a JIRA issue via AgileGuard.
 * Includes all mandatory and enriched fields added in this release.
 *
 * Field mapping to JIRA REST API v3:
 *   summary           → fields.summary
 *   description       → fields.description (ADF format in real calls)
 *   acceptanceCriteria→ fields.customfield_10014 (AC custom field)
 *   issueType         → fields.issuetype.name
 *   priority          → fields.priority.name
 *   storyPoints       → fields.customfield_10016
 *   sprintId          → fields.customfield_10020 (sprint)
 *   epicLink          → fields.customfield_10014 or parent.key
 *   components        → fields.components[].name
 *   fixVersions       → fields.fixVersions[].name
 *   labels            → fields.labels[]
 *   reporterAccountId → fields.reporter.accountId
 *   businessLine      → fields.customfield_10100 (example custom field)
 *   teamName          → fields.customfield_10101 (example custom field)
 */
@Data
public class CreateIssueRequest {

    @NotBlank(message = "Project key is required")
    private String projectKey;

    @NotBlank(message = "Summary is required")
    @Size(min = 5, max = 255, message = "Summary must be 5–255 characters")
    private String summary;

    @NotBlank(message = "Description is required")
    @Size(min = 20, message = "Description must be at least 20 characters")
    private String description;

    @NotBlank(message = "Acceptance Criteria is required")
    private String acceptanceCriteria;

    @NotBlank(message = "Issue type is required")
    private String issueType;       // Story | Bug | Task

    @NotBlank(message = "Priority is required")
    private String priority;        // Blocker | Critical | Major | Medium | Minor | Low | Trivial

    @NotBlank(message = "Business line is required")
    private String businessLine;    // Business | GT | Platform | Infrastructure | Data

    private Integer storyPoints;
    private Long    sprintId;       // sprint numeric ID from JIRA
    private String  sprintName;     // for display only
    private String  epicLink;       // epic issue key e.g. "COMMSSURV-5"
    private String  epicName;       // resolved name, stored for display
    private String  reporterAccountId;
    private String  reporterName;

    private List<String> components;    // component names
    private List<String> fixVersions;   // version names
    private List<String> labels;        // free-text labels
    private List<String> teamNames;     // team names (custom field)

    private String assigneeAccountId;
    private String assigneeName;
}
