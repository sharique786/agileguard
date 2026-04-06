package com.agileguard.jira.model;

import com.agileguard.common.enums.IssueStatus;
import com.agileguard.common.enums.IssueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JIRA issue as returned by the Atlassian REST API v3.
 * Updated to include all enriched fields: priority, component/s, labels,
 * business line, sprint, team names, reporter, fix version/s, and epic link.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssue {

    // ── Core fields (existing) ─────────────────────────────────────────────
    private String id;
    private String key;

    private String summary;
    private String description;
    private String acceptanceCriteria;

    @Builder.Default
    private IssueType issueType = IssueType.STORY;

    @Builder.Default
    private IssueStatus status = IssueStatus.TO_DO;

    private Integer storyPoints;
    private String  assigneeEmail;
    private String  assigneeName;
    private String  reporterEmail;
    private String  projectKey;

    /** Sprint ID (numeric) */
    private Long    sprintId;
    /** Sprint display name */
    private String  sprintName;

    private String  component;
    private String  componentVersion;
    private Integer timeSpentMinutes;
    private LocalDate statusChangedAt;
    private LocalDate createdAt;

    @Builder.Default
    private List<JiraSubTask> subTasks = new ArrayList<>();

    @Builder.Default
    private List<String> linkedPullRequests = new ArrayList<>();

    // ── New enriched fields ────────────────────────────────────────────────

    /**
     * Issue priority: Blocker | Critical | Major | Medium | Minor | Low | Trivial
     * Maps to JIRA fields.priority.name
     */
    private String priority;

    /**
     * Business line classification.
     * Maps to a JIRA custom field (e.g., customfield_10100).
     * Values: Business | GT | Platform | Infrastructure | Data | Security
     */
    private String businessLine;

    /**
     * Epic link — the key of the parent Epic (e.g., "COMMSSURV-5").
     * Maps to JIRA fields.customfield_10014 or parent.key (next-gen projects).
     */
    private String epicLink;

    /**
     * Resolved epic name — populated by fetching the epic's summary.
     * Stored so the UI can display it without a second API call.
     */
    private String epicName;

    /**
     * Reporter account ID (Atlassian account ID).
     * Maps to JIRA fields.reporter.accountId.
     */
    private String reporterAccountId;

    /** Reporter display name for UI rendering. */
    private String reporterName;

    /**
     * List of component names attached to this issue.
     * Maps to JIRA fields.components[].name.
     */
    @Builder.Default
    private List<String> components = new ArrayList<>();

    /**
     * List of fix version names.
     * Maps to JIRA fields.fixVersions[].name.
     */
    @Builder.Default
    private List<String> fixVersions = new ArrayList<>();

    /**
     * Free-text labels / tags applied to the issue.
     * Maps to JIRA fields.labels[].
     */
    @Builder.Default
    private List<String> labels = new ArrayList<>();

    /**
     * Team name(s) responsible for this issue.
     * Maps to a JIRA custom field (e.g., customfield_10101 / Team field).
     */
    @Builder.Default
    private List<String> teamNames = new ArrayList<>();

    // ── Derived helpers ────────────────────────────────────────────────────

    public boolean hasDevSubTask() {
        return subTasks.stream().anyMatch(s ->
                s.getType().equalsIgnoreCase("Dev Task") ||
                s.getType().equalsIgnoreCase("Development"));
    }

    public boolean hasQaSubTask() {
        return subTasks.stream().anyMatch(s ->
                s.getType().equalsIgnoreCase("QA Task") ||
                s.getType().equalsIgnoreCase("Testing"));
    }

    public long getDaysInCurrentStatus() {
        if (statusChangedAt == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(statusChangedAt, LocalDate.now());
    }
}
