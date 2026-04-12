package com.agileguard.jira.controller;

import com.agileguard.common.dto.ApiResponse;
import com.agileguard.jira.model.*;
import com.agileguard.jira.service.EpicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Epic management endpoints — sole owner of /api/jira/epics/**
 *
 *   POST /api/jira/epics
 *       Create a new JIRA Epic from the Story Editor "Create New Epic" form.
 *       Requires: projectKey, epicName. Optional: description, priority, businessLine.
 *       Returns the created JiraIssue (including the generated issue key).
 *
 *   GET  /api/jira/epics/{epicKey}/inspect
 *       Inspect an existing epic: fetch all linked stories, run gap detection
 *       on each, and return aggregated quality metrics.
 *       Called when the user enters an epic key in "Use Existing Epic" mode.
 *       Returns EpicInspectionResult (field names match Angular template).
 *
 *   GET  /api/jira/epics/{epicKey}/stories
 *       Return the list of stories linked to an epic without gap analysis.
 *       Used to feed existing story context into AI prompts.
 *
 * Note: GET /api/jira/epics/{epicKey} (resolve epic key → name) is handled
 *       by JiraController via JiraLookupService — it remains there because
 *       it is also used by the Story Editor "Epic Link" field resolver.
 */
@RestController
@RequestMapping("/api/jira/epics")
@RequiredArgsConstructor
@Slf4j
public class EpicController {

    private final EpicService epicService;

    // ── Create Epic ──────────────────────────────────────────────────────────

    /**
     * Creates a new JIRA Epic issue and returns it.
     *
     * Called when the user clicks "🏗 Create Epic JIRA" in the Story Editor.
     * The returned issue key is displayed in the confirmation banner and used
     * as the epicLink when generating child stories.
     *
     * Request body:
     * {
     *   "projectKey":   "PLAT",
     *   "epicName":     "Payments Gateway v2 — Multi-currency support",
     *   "description":  "As a merchant...",
     *   "priority":     "High",
     *   "businessLine": "Business",
     *   "reporterAccountId": "acc-001",  // optional
     *   "reporterName":      "Priya Sharma" // optional
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<JiraIssue>> createEpic(
            @Valid @RequestBody CreateEpicRequest request) {

        log.info("Creating epic '{}' in project {}",
                request.getEpicName(), request.getProjectKey());

        JiraIssue created = epicService.createEpic(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Epic created: " + created.getKey() + " — " + created.getSummary(),
                        created));
    }

    // ── Inspect Existing Epic ────────────────────────────────────────────────

    /**
     * Inspects an existing epic: fetches all linked stories, runs AgileGuard
     * gap detection on every story, and returns aggregated quality metrics.
     *
     * Called when the user clicks "🔍 Fetch & Inspect" in "Use Existing Epic" mode.
     *
     * Returns EpicInspectionResult with these Angular-template-facing fields:
     *   storyCount          — total stories linked
     *   averageQualityScore — average gap-detection score (0–100)
     *   flaggedStoryCount   — stories with score < 40 or CRITICAL finding
     *   gapCount            — total gap findings across all stories
     *   gapReports          — per-story gap analysis for the table
     *   topGaps             — human-readable list of the worst gap types
     *   gapSummary          — narrative quality summary
     */
    @GetMapping("/{epicKey}/inspect")
    public ResponseEntity<ApiResponse<EpicInspectionResult>> inspectEpic(
            @PathVariable String epicKey) {

        log.info("Inspection requested for epic: {}", epicKey);
        EpicInspectionResult result = epicService.inspectEpic(epicKey);

        return ResponseEntity.ok(ApiResponse.success(
                "Epic inspection complete for " + epicKey, result));
    }

    // ── Get Epic Stories ─────────────────────────────────────────────────────

    /**
     * Returns all stories linked to an epic without running gap detection.
     * Lighter than /inspect — used by the AI service to load epic context
     * when generating Acceptance Criteria for a new story.
     */
    @GetMapping("/{epicKey}/stories")
    public ResponseEntity<ApiResponse<List<JiraIssue>>> getEpicStories(
            @PathVariable String epicKey) {

        log.info("Fetching stories for epic: {}", epicKey);
        List<JiraIssue> stories = epicService.getStoriesForEpic(epicKey);

        return ResponseEntity.ok(ApiResponse.success(
                stories.size() + " stories found for epic " + epicKey, stories));
    }
}
