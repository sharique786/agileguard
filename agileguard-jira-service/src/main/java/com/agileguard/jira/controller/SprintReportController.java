package com.agileguard.jira.controller;

import com.agileguard.common.dto.ApiResponse;
import com.agileguard.jira.model.JiraSprint;
import com.agileguard.jira.model.PiSprintReport;
import com.agileguard.jira.service.SprintReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for SAFe sprint reports.
 *
 * GET /api/jira/sprint-report
 *   Generates full programme-level sprint report for all feature teams.
 *   Query params:
 *     projectKey  (required)  — JIRA project key, e.g. "PLAT"
 *     sprintId    (optional)  — numeric sprint ID; omit for active sprint
 *     pi          (optional)  — PI label e.g. "PI-7" for display only
 *
 * GET /api/jira/sprint-report/sprints
 *   Returns all sprints for a project (active + closed + future).
 *   Used to populate the sprint selector in the UI.
 *   Query params:
 *     projectKey (required)
 */
@RestController
@RequestMapping("/api/jira/sprint-report")
@RequiredArgsConstructor
public class SprintReportController {

    private final SprintReportService sprintReportService;

    /**
     * Returns the full SAFe sprint report aggregated across all feature teams.
     * When sprintId is omitted the active sprint is used.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PiSprintReport>> getSprintReport(
            @RequestParam String projectKey,
            @RequestParam(required = false) Long sprintId,
            @RequestParam(required = false, defaultValue = "") String pi) {

        PiSprintReport report = sprintReportService.generateSprintReport(projectKey, sprintId, pi);
        return ResponseEntity.ok(ApiResponse.success("Sprint report generated", report));
    }

    /**
     * Returns the list of sprints for the sprint selector dropdown.
     */
    @GetMapping("/sprints")
    public ResponseEntity<ApiResponse<List<JiraSprint>>> getSprintsForProject(
            @RequestParam String projectKey) {

        List<JiraSprint> sprints = sprintReportService.getSprintsForProject(projectKey);
        return ResponseEntity.ok(ApiResponse.success("Sprints retrieved", sprints));
    }
}
