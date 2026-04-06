package com.agileguard.github.controller;

import com.agileguard.common.dto.ApiResponse;
import com.agileguard.github.model.*;
import com.agileguard.github.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for GitHub integration: workflow runs, SDLC gates, leaderboard.
 */
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubClientService githubClient;
    private final SdlcControlService sdlcService;
    private final LeaderboardService leaderboardService;

    /** Returns recent GitHub Actions runs for a repository. */
    @GetMapping("/repos/{owner}/{repo}/runs")
    public ResponseEntity<ApiResponse<List<WorkflowRun>>> getWorkflowRuns(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "20") int maxResults) {
        return ResponseEntity.ok(ApiResponse.success(
                githubClient.getWorkflowRuns(owner, repo, maxResults)));
    }

    /** Checks all SDLC control gates for a JIRA issue. */
    @GetMapping("/sdlc/{jiraIssueKey}")
    public ResponseEntity<ApiResponse<SdlcStatus>> getSdlcStatus(
            @PathVariable String jiraIssueKey,
            @RequestParam(defaultValue = "db-platform") String owner,
            @RequestParam(defaultValue = "payments-service") String repo) {
        SdlcStatus status = sdlcService.checkSdlcStatus(jiraIssueKey, owner, repo);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /** Returns the developer and QA leaderboard for a project. */
    @GetMapping("/leaderboard/{projectId}")
    public ResponseEntity<ApiResponse<List<LeaderboardEntry>>> getLeaderboard(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "db-platform") String githubOrg) {
        return ResponseEntity.ok(ApiResponse.success(
                leaderboardService.getLeaderboard(projectId, githubOrg)));
    }
}
