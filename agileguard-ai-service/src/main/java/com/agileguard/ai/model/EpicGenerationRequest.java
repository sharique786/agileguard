package com.agileguard.ai.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request payload for AI-powered epic decomposition.
 *
 * The AI uses the epic description written in plain English plus the
 * team configuration to:
 *   1. Determine how many stories are needed to implement the epic
 *   2. Distribute work across developers considering standard bandwidth
 *   3. Generate fully-formed stories with all required AgileGuard fields
 */
@Data
public class EpicGenerationRequest {

    // ── Epic details ────────────────────────────────────────────────────────
    @NotBlank(message = "Epic title is required")
    private String epicTitle;

    @NotBlank(message = "Epic description in plain English is required")
    private String epicDescription;

    /** Existing JIRA epic key if already created (e.g., "COMMSSURV-10"). Null if new. */
    private String epicKey;

    private String projectKey;
    private String businessLine;     // Business | GT | Platform | Infrastructure | Data | Security
    private String priority;         // Default priority for generated stories
    private String sprintName;       // Target sprint for the generated stories
    private Long   sprintId;

    // ── Team configuration ────────────────────────────────────────────────
    @NotNull(message = "Number of developers is required")
    @Min(value = 1, message = "At least 1 developer is required")
    private Integer numberOfDevelopers;

    @NotNull(message = "Number of QA engineers is required")
    @Min(value = 0, message = "QA count cannot be negative")
    private Integer numberOfQaEngineers;

    /**
     * Assumed developer bandwidth in story points per sprint (default 8).
     * Used to estimate how many stories can be completed in one sprint.
     */
    private Integer developerBandwidthSp = 8;

    private List<String> components;     // JIRA components to assign
    private List<String> fixVersions;    // Target fix versions
    private List<String> teamNames;      // Feature team names
    private String       reporterName;   // Reporter to assign to all generated stories
}
