package com.agileguard.jira.controller;

import com.agileguard.common.dto.ApiResponse;
import com.agileguard.common.enums.IssueStatus;
import com.agileguard.jira.model.*;
import com.agileguard.jira.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for JIRA integration endpoints.
 * All endpoints require a tenant ID in the request header (set by gateway).
 */
//@RestController
//@RequestMapping("/api/jira")
@RequiredArgsConstructor
public class JiraController_bkp {

    private final JiraClientService jiraClientService;
    private final GapReportService gapReportService;
    private final StoryGapDetector gapDetector;
    private final TransitionGuard transitionGuard;

    /**
     * Returns all active sprint issues for a project with gap scores.
     */
    @GetMapping("/projects/{projectKey}/issues")
    public ResponseEntity<ApiResponse<List<JiraIssue>>> getActiveSprintIssues(
            @PathVariable String projectKey) {
        List<JiraIssue> issues = jiraClientService.getActiveSprintIssues(projectKey);
        return ResponseEntity.ok(ApiResponse.success(issues));
    }

    /**
     * Fetches a single JIRA issue and evaluates its gap score.
     */
    @GetMapping("/issues/{issueKey}")
    public ResponseEntity<ApiResponse<JiraIssue>> getIssue(@PathVariable String issueKey) {
        Optional<JiraIssue> issue = jiraClientService.getIssue(issueKey);
        return issue.map(i -> ResponseEntity.ok(ApiResponse.success(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Evaluates the quality score for a single issue.
     */
    @GetMapping("/issues/{issueKey}/gap-report")
    public ResponseEntity<ApiResponse<GapReport>> getIssueGapReport(
            @PathVariable String issueKey) {
        Optional<JiraIssue> issue = jiraClientService.getIssue(issueKey);
        if (issue.isEmpty()) return ResponseEntity.notFound().build();
        GapReport report = gapDetector.evaluate(issue.get());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Validates whether a status transition is allowed.
     * Returns 200 with allowed=true, or 400 with violation details.
     */
    @PostMapping("/issues/{issueKey}/validate-transition")
    public ResponseEntity<ApiResponse<String>> validateTransition(
            @PathVariable String issueKey,
            @RequestParam IssueStatus toStatus) {
        Optional<JiraIssue> issue = jiraClientService.getIssue(issueKey);
        if (issue.isEmpty()) return ResponseEntity.notFound().build();
        transitionGuard.validateTransition(issue.get(), toStatus);
        return ResponseEntity.ok(ApiResponse.success("Transition allowed", null));
    }

    /**
     * Triggers a full gap scan for a project's active sprint.
     * Returns a SprintHealthReport with all findings.
     */
    @PostMapping("/projects/{projectKey}/scan")
    public ResponseEntity<ApiResponse<SprintHealthReport>> scanProject(
            @PathVariable String projectKey,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "demo") String tenantId) {
        SprintHealthReport report = gapReportService.scanActiveSprintGaps(projectKey, tenantId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Returns all open gap findings for the tenant.
     */
    @GetMapping("/gaps")
    public ResponseEntity<ApiResponse<?>> getOpenGaps(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "demo") String tenantId) {
        return ResponseEntity.ok(
                ApiResponse.success(gapReportService.getOpenGaps(tenantId)));
    }
}
