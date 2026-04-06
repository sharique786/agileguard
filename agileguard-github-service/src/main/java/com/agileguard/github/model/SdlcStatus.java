package com.agileguard.github.model;

import lombok.*;
import java.util.List;

/** SDLC control gate status for a JIRA story. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SdlcStatus {
    private String jiraIssueKey;
    private boolean allGatesPassed;
    private boolean unitTestsPassed;
    private boolean integrationTestsPassed;
    private boolean coverageThresholdMet;
    private boolean securityScanPassed;
    private boolean prMerged;
    private boolean prReviewed;
    private List<String> failedGates;
    private List<WorkflowRun> recentRuns;
}
