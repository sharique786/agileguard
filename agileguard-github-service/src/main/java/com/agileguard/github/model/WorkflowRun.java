package com.agileguard.github.model;

import lombok.*;
import java.time.Instant;

/** Represents a GitHub Actions workflow run. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowRun {
    private Long id;
    private String name;
    private String headBranch;
    private String headSha;
    private String status;        // queued, in_progress, completed
    private String conclusion;    // success, failure, cancelled, skipped
    private String workflowName;
    private String repoFullName;
    private String triggeredBy;
    private Instant startedAt;
    private Instant completedAt;
    private long durationSeconds;
    private String jiraIssueKey;  // extracted from commit message / branch name
    private String htmlUrl;
}
