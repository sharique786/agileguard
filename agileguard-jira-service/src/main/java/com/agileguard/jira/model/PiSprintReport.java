package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated SAFe sprint report for an entire project —
 * contains one TeamSprintReport per feature team that participated in the sprint.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PiSprintReport {

    private String  projectKey;
    private String  piName;
    private Long    sprintId;
    private String  sprintName;
    private String  sprintState;
    private String  sprintGoal;
    private String  sprintStartDate;
    private String  sprintEndDate;

    // ── Programme-level aggregates ─────────────────────────────────────────

    /** Sum of storyPointsCommitted across all teams. */
    private int totalPointsCommitted;

    /** Sum of storyPointsAccepted across all teams. */
    private int totalPointsAccepted;

    /** Programme-level predictability: (totalAccepted / totalCommitted) * 100. */
    private Double programmePredictability;

    /** Sum of velocities across teams. */
    private int totalVelocity;

    /** Total stories planned across teams. */
    private int totalStoriesPlanned;

    /** Total stories accepted across teams. */
    private int totalStoriesAccepted;

    /** Total stories spilled across teams. */
    private int totalStoriesSpilled;

    /** Total planned capacity hours across teams. */
    private double totalCapacityPlanned;

    /** Total actual capacity hours across teams. */
    private double totalCapacityActual;

    // ── Per-team reports ───────────────────────────────────────────────────

    @Builder.Default
    private List<TeamSprintReport> teamReports = new ArrayList<>();

    private Instant generatedAt;
}
