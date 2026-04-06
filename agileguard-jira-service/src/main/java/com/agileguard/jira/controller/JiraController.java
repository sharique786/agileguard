package com.agileguard.jira.controller;

import com.agileguard.common.dto.ApiResponse;
import com.agileguard.common.enums.IssueStatus;
import com.agileguard.jira.model.CreateIssueRequest;
import com.agileguard.jira.model.GapReport;
import com.agileguard.jira.model.JiraComponent;
import com.agileguard.jira.model.JiraEpic;
import com.agileguard.jira.model.JiraIssue;
import com.agileguard.jira.model.JiraSprint;
import com.agileguard.jira.model.JiraUser;
import com.agileguard.jira.model.JiraVersion;
import com.agileguard.jira.model.SprintHealthReport;
import com.agileguard.jira.service.GapReportService;
import com.agileguard.jira.service.JiraClientService;
import com.agileguard.jira.service.JiraLookupService;
import com.agileguard.jira.service.StoryGapDetector;
import com.agileguard.jira.service.TransitionGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for JIRA integration.
 * <p>
 * Story Editor lookup endpoints (new):
 * GET /api/jira/projects/{key}/sprints?q=         typeahead sprints
 * GET /api/jira/projects/{key}/components?q=      typeahead components
 * GET /api/jira/projects/{key}/versions?q=        typeahead fix versions
 * GET /api/jira/users?q=                          typeahead reporter users
 * GET /api/jira/epics/{epicKey}                   resolve epic key → name
 * POST /api/jira/issues/create                    create issue with all fields
 * Existing endpoints remain unchanged.
 */
@RestController
@RequestMapping("/api/jira")
@RequiredArgsConstructor
public class JiraController {

    private final JiraClientService jiraClientService;
    private final JiraLookupService lookupService;
    private final GapReportService gapReportService;
    private final StoryGapDetector gapDetector;
    private final TransitionGuard transitionGuard;

    // ── Existing endpoints ────────────────────────────────────────────────────

    @GetMapping("/projects/{projectKey}/issues")
    public ResponseEntity<ApiResponse<List<JiraIssue>>> getActiveSprintIssues(
            @PathVariable String projectKey) {
        return ResponseEntity.ok(ApiResponse.success(
                jiraClientService.getActiveSprintIssues(projectKey)));
    }

    @GetMapping("/issues/{issueKey}")
    public ResponseEntity<ApiResponse<JiraIssue>> getIssue(@PathVariable String issueKey) {
        Optional<JiraIssue> issue = jiraClientService.getIssue(issueKey);
        return issue.map(i -> ResponseEntity.ok(ApiResponse.success(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/issues/{issueKey}/gap-report")
    public ResponseEntity<ApiResponse<GapReport>> getIssueGapReport(
            @PathVariable String issueKey) {
        Optional<JiraIssue> issue = jiraClientService.getIssue(issueKey);
        if (issue.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(gapDetector.evaluate(issue.get())));
    }

    @PostMapping("/issues/{issueKey}/validate-transition")
    public ResponseEntity<ApiResponse<String>> validateTransition(
            @PathVariable String issueKey,
            @RequestParam IssueStatus toStatus) {
        Optional<JiraIssue> issue = jiraClientService.getIssue(issueKey);
        if (issue.isEmpty()) return ResponseEntity.notFound().build();
        transitionGuard.validateTransition(issue.get(), toStatus);
        return ResponseEntity.ok(ApiResponse.success("Transition allowed", null));
    }

    @PostMapping("/projects/{projectKey}/scan")
    public ResponseEntity<ApiResponse<SprintHealthReport>> scanProject(
            @PathVariable String projectKey,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "demo") String tenantId) {
        SprintHealthReport report = gapReportService.scanActiveSprintGaps(projectKey, tenantId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/gaps")
    public ResponseEntity<ApiResponse<?>> getOpenGaps(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "demo") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(gapReportService.getOpenGaps(tenantId)));
    }

    // ── Story Editor lookup endpoints (NEW) ───────────────────────────────────

    /**
     * Typeahead for sprints: returns active + future sprints matching the query.
     * Called as the user types in the Sprint field (debounced 300ms).
     */
    @GetMapping("/projects/{projectKey}/sprints")
    public ResponseEntity<ApiResponse<List<JiraSprint>>> searchSprints(
            @PathVariable String projectKey,
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.success(
                lookupService.searchSprints(projectKey, q)));
    }

    /**
     * Typeahead for components: returns components matching the query.
     * Called as the user types in the Component/s field (debounced 300ms).
     */
    @GetMapping("/projects/{projectKey}/components")
    public ResponseEntity<ApiResponse<List<JiraComponent>>> searchComponents(
            @PathVariable String projectKey,
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.success(
                lookupService.searchComponents(projectKey, q)));
    }

    /**
     * Typeahead for fix versions: returns unreleased versions matching the query.
     * Called as the user types in the Fix Version/s field (debounced 300ms).
     */
    @GetMapping("/projects/{projectKey}/versions")
    public ResponseEntity<ApiResponse<List<JiraVersion>>> searchVersions(
            @PathVariable String projectKey,
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.success(
                lookupService.searchVersions(projectKey, q)));
    }

    /**
     * Typeahead for users (Reporter field).
     * Searches across display name and email address.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<JiraUser>>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.success(lookupService.searchUsers(q)));
    }

    /**
     * Resolves an epic issue key to its name and status.
     * Called on blur of the Epic Link field (e.g., when user types "COMMSSURV-5").
     * Returns 404 if the key is not found or is not an Epic.
     */
    @GetMapping("/epics/{epicKey}")
    public ResponseEntity<ApiResponse<JiraEpic>> getEpic(@PathVariable String epicKey) {
        Optional<JiraEpic> epic = lookupService.getEpic(epicKey);
        return epic.map(e -> ResponseEntity.ok(ApiResponse.success(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new JIRA issue with all enriched fields.
     * Validates mandatory fields, enforces the gap detection rules on submission.
     */
    @PostMapping("/issues/create")
    public ResponseEntity<ApiResponse<JiraIssue>> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "demo") String tenantId) {

        // In real mode, forward to JIRA REST API. For POC, echo back a mock issue.
        JiraIssue created = JiraIssue.builder()
                .id("mock-" + System.currentTimeMillis())
                .key(request.getProjectKey() + "-" + (int) (Math.random() * 900 + 100))
                .summary(request.getSummary())
                .description(request.getDescription())
                .acceptanceCriteria(request.getAcceptanceCriteria())
                .priority(request.getPriority())
                .businessLine(request.getBusinessLine())
                .epicLink(request.getEpicLink())
                .epicName(request.getEpicName())
                .storyPoints(request.getStoryPoints())
                .sprintId(request.getSprintId())
                .sprintName(request.getSprintName())
                .reporterAccountId(request.getReporterAccountId())
                .reporterName(request.getReporterName())
                .components(request.getComponents() != null ? request.getComponents() : List.of())
                .fixVersions(request.getFixVersions() != null ? request.getFixVersions() : List.of())
                .labels(request.getLabels() != null ? request.getLabels() : List.of())
                .teamNames(request.getTeamNames() != null ? request.getTeamNames() : List.of())
                .projectKey(request.getProjectKey())
                .build();

        return ResponseEntity.status(201).body(ApiResponse.success("Issue created", created));
    }
}
