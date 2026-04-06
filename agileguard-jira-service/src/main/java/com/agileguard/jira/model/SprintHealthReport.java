package com.agileguard.jira.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprint-level health report aggregating all issue gap reports.
 */
@Data
@Builder
public class SprintHealthReport {

    private String sprintId;
    private String sprintName;
    private String projectKey;
    private String tenantId;

    private int totalIssues;
    private int issuesWithGaps;
    private int flaggedIssues;
    private double averageQualityScore;
    private int overallHealthScore;     // 0-100

    private List<GapReport> issueReports = new ArrayList<>();
    private List<String> agingIssueKeys = new ArrayList<>();   // issues in same status > threshold

    @Builder.Default
    private Instant generatedAt = Instant.now();
}
