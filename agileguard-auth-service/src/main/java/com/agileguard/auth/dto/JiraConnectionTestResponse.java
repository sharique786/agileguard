package com.agileguard.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Result of testing JIRA credentials before saving. */
@Data
@Builder
public class JiraConnectionTestResponse {
    private boolean connected;
    private String message;
    private String jiraAccountId;
    private String displayName;
    private List<String> accessibleProjects;
}
