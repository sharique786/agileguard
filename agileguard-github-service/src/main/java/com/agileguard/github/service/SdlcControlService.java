package com.agileguard.github.service;

import com.agileguard.github.model.SdlcStatus;
import com.agileguard.github.model.WorkflowRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates SDLC control gates for a JIRA issue by checking
 * GitHub Actions workflow results and PR status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SdlcControlService {

    private final GitHubClientService githubClient;

    /**
     * Checks all SDLC gates for a given JIRA issue key.
     *
     * @param jiraIssueKey  JIRA issue key (e.g., "COMMSSURV-101")
     * @param owner         GitHub org or user
     * @param repo          repository name
     * @return SdlcStatus with individual gate results
     */
    public SdlcStatus checkSdlcStatus(String jiraIssueKey, String owner, String repo) {
        log.info("Checking SDLC gates for {} in {}/{}", jiraIssueKey, owner, repo);

        List<WorkflowRun> runs = githubClient.getWorkflowRuns(owner, repo, 20);

        // Filter runs for this issue (by branch name or commit message convention)
        List<WorkflowRun> issueRuns = runs.stream()
                .filter(r -> jiraIssueKey.equalsIgnoreCase(r.getJiraIssueKey())
                          || (r.getHeadBranch() != null &&
                              r.getHeadBranch().toLowerCase().contains(jiraIssueKey.toLowerCase())))
                .toList();

        boolean ciPassed = issueRuns.stream()
                .filter(r -> "CI Pipeline".equalsIgnoreCase(r.getWorkflowName()))
                .anyMatch(r -> "success".equals(r.getConclusion()));

        boolean securityPassed = runs.stream()
                .filter(r -> r.getWorkflowName() != null &&
                             r.getWorkflowName().toLowerCase().contains("security"))
                .anyMatch(r -> "success".equals(r.getConclusion()));

        List<String> failed = new ArrayList<>();
        if (!ciPassed) failed.add("CI Pipeline not passed for this issue's branch");
        if (!securityPassed) failed.add("Security scan has not passed");

        return SdlcStatus.builder()
                .jiraIssueKey(jiraIssueKey)
                .unitTestsPassed(ciPassed)
                .integrationTestsPassed(ciPassed)
                .coverageThresholdMet(ciPassed)
                .securityScanPassed(securityPassed)
                .prMerged(!issueRuns.isEmpty())
                .prReviewed(!issueRuns.isEmpty())
                .allGatesPassed(failed.isEmpty())
                .failedGates(failed)
                .recentRuns(issueRuns.isEmpty() ? runs.subList(0, Math.min(3, runs.size())) : issueRuns)
                .build();
    }
}
