package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SAFe-compliant sprint report for a single feature team.
 *
 * Core SAFe metrics:
 *   Predictability  = (storyPointsAccepted / storyPointsCommitted) * 100
 *   Velocity        = storyPointsCompleted (completed = accepted in SAFe)
 *
 * Capacity model:
 *   Capacity Planned  = teamSize * hoursPerPersonPerSprint
 *   Capacity Actual   = total effort logged (timeSpentMinutes / 60) across all stories
 *   Capacity Utilisation = (capacityActual / capacityPlanned) * 100
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TeamSprintReport {

    // ── Identification ─────────────────────────────────────────────────────
    private String    featureTeamId;
    private String    featureTeamName;      // "Payments Team"
    private String    projectKey;           // "COMMSSURV"
    private String    jiraComponent;        // "Payments"
    private String    piName;               // "PI-7"
    private Long      sprintId;
    private String    sprintName;           // "COMMSSURV Sprint 42 — Platform Hardening"
    private String    sprintGoal;
    private LocalDate sprintStartDate;
    private LocalDate sprintEndDate;
    private String    sprintState;          // active | closed

    // ── SAFe Core Metrics ──────────────────────────────────────────────────

    /** Total story points planned at sprint start (all stories pulled into sprint). */
    private int storyPointsCommitted;

    /** Story points of all stories that reached DONE status. */
    private int storyPointsCompleted;

    /** Same as storyPointsCompleted in SAFe (accepted = DONE). */
    private int storyPointsAccepted;

    /** Story points still in progress or spilled. */
    private int storyPointsSpilled;

    /** (storyPointsAccepted / storyPointsCommitted) * 100. Null if committed = 0. */
    private Double predictability;

    /** storyPointsCompleted — sprint velocity. */
    private int velocity;

    // ── Story Counts ───────────────────────────────────────────────────────

    /** Total stories pulled into the sprint at planning. */
    private int storiesPlanned;

    /** Stories that reached DONE status. */
    private int storiesAccepted;

    /** Stories not completed — spilled or in progress. */
    private int storiesSpilled;

    /** Stories added to sprint after start (scope creep). */
    private int storiesAdded;

    /** Stories removed from sprint before end. */
    private int storiesRemoved;

    // ── Capacity ───────────────────────────────────────────────────────────

    /** Planned capacity in hours (team size × hours-per-person per sprint). */
    private double capacityPlanned;

    /** Actual effort logged across all sprint stories (converted from minutes). */
    private double capacityActual;

    /** (capacityActual / capacityPlanned) * 100. Null if planned = 0. */
    private Double capacityUtilisation;

    /** Number of active team members in this sprint. */
    private int teamSize;

    // ── Story Lists ────────────────────────────────────────────────────────

    /** Stories that reached DONE — shown in "Completed" section. */
    @Builder.Default
    private List<SprintStoryDetail> completedStories = new ArrayList<>();

    /** Stories not completed — shown in "Spilled / Moved Out" section. */
    @Builder.Default
    private List<SprintStoryDetail> spilledStories = new ArrayList<>();

    /** Stories added mid-sprint (scope creep indicator). */
    @Builder.Default
    private List<SprintStoryDetail> addedStories = new ArrayList<>();

    // ── Health Indicators ──────────────────────────────────────────────────

    /** True when predictability >= 80%. SAFe target. */
    private boolean meetsPredictabilityTarget;

    /** Trend indicator vs previous sprint: UP | DOWN | STABLE | UNKNOWN. */
    private String velocityTrend;

    /** Short-form narrative summary e.g. "Strong sprint, 2 stories spilled due to dependency." */
    private String summary;

    /** Report generation timestamp. */
    private java.time.Instant generatedAt;
}
