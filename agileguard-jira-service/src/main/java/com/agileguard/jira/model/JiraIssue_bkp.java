package com.agileguard.jira.model;

import com.agileguard.common.enums.IssueStatus;
import com.agileguard.common.enums.IssueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JIRA issue as returned by the Atlassian REST API v3.
 * Maps to the JIRA issue structure with fields relevant for gap detection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssue_bkp {

    private String id;
    private String key;                  // e.g., "PROJ-123"
    private String summary;
    private String description;
    private String acceptanceCriteria;

    @Builder.Default
    private IssueType issueType = IssueType.STORY;

    @Builder.Default
    private IssueStatus status = IssueStatus.TO_DO;

    private Integer storyPoints;
    private String assigneeEmail;
    private String assigneeName;
    private String reporterEmail;
    private String projectKey;
    private String sprintId;
    private String sprintName;
    private String component;
    private String componentVersion;
    private Integer timeSpentMinutes;    // effort logged
    private LocalDate statusChangedAt;
    private LocalDate createdAt;

    @Builder.Default
    private List<JiraSubTask> subTasks = new ArrayList<>();

    @Builder.Default
    private List<String> linkedPullRequests = new ArrayList<>();

    /** Returns true if this issue has a developer sub-task. */
    public boolean hasDevSubTask() {
        return subTasks.stream()
                .anyMatch(s -> s.getType().equalsIgnoreCase("Dev Task")
                            || s.getType().equalsIgnoreCase("Development"));
    }

    /** Returns true if this issue has a QA sub-task. */
    public boolean hasQaSubTask() {
        return subTasks.stream()
                .anyMatch(s -> s.getType().equalsIgnoreCase("QA Task")
                            || s.getType().equalsIgnoreCase("Testing"));
    }

    /** Returns the number of days the issue has been in its current status. */
    public long getDaysInCurrentStatus() {
        if (statusChangedAt == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(statusChangedAt, LocalDate.now());
    }
}
