package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a JIRA sub-task linked to a parent issue. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JiraSubTask {
    private String id;
    private String key;
    private String summary;
    private String type;        // "Dev Task", "QA Task", "Documentation"
    private String status;
    private Integer timeSpentMinutes;
    private String assigneeEmail;
}
