package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Full quality inspection result for an existing JIRA Epic.
 *
 * Returned by GET /api/jira/epics/{epicKey}/inspect
 *
 * Angular template field names this object must satisfy:
 *   insp.epicKey
 *   insp.epicName
 *   insp.epicStatus
 *   insp.storyCount          ← total linked stories
 *   insp.averageQualityScore ← 0-100
 *   insp.flaggedStoryCount   ← stories with score < 40 or any CRITICAL finding
 *   insp.gapReports          ← per-story GapReport list (rendered in the gap rows table)
 *   insp.topGaps             ← short human-readable gap summary strings
 *   insp.gapCount            ← total gap findings across all stories
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EpicInspectionResult {

    // ── Epic identity ──────────────────────────────────────────────────────
    private String epicKey;
    private String epicName;
    private String epicStatus;

    // ── Aggregated metrics ─────────────────────────────────────────────────

    /** Total number of stories currently linked to this epic. */
    private int storyCount;

    /** Average quality score (0–100) across all linked stories. */
    private double averageQualityScore;

    /** Stories whose score is below 40 or contain a CRITICAL finding. */
    private int flaggedStoryCount;

    /** Total gap findings across all stories. */
    private int gapCount;

    /** Stories that have no Acceptance Criteria. */
    private int missingAcCount;

    /** Stories with no story point estimate. */
    private int missingPointsCount;

    /** Stories stuck in the same status beyond the aging threshold. */
    private int agingCount;

    // ── Gap details ────────────────────────────────────────────────────────

    /**
     * Per-story gap reports — one GapReport per linked story.
     * The Angular gap row table iterates over this list.
     */
    @Builder.Default
    private List<GapReport> gapReports = new ArrayList<>();

    /**
     * Short human-readable gap descriptions for the summary panel.
     * Example: "5 stories missing Acceptance Criteria",
     *          "2 stories aging > 5 days",
     *          "Overall quality below team average"
     */
    @Builder.Default
    private List<String> topGaps = new ArrayList<>();

    /**
     * Full linked story list (lightweight — same data as gap reports
     * but without the gap analysis detail; useful for AI context).
     */
    @Builder.Default
    private List<JiraIssue> linkedStories = new ArrayList<>();

    /** Narrative summary of the epic's quality health. */
    private String gapSummary;

    /** Timestamp of when this inspection was run. */
    @Builder.Default
    private Instant inspectedAt = Instant.now();
}
