package com.agileguard.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI-generated enhancement suggestions for an existing story.
 * Shows what to improve and why.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StoryEnhancementResponse {

    private String issueKey;
    private int qualityScore;          // current score (0–100)
    private int improvedScore;         // projected score after applying suggestions

    private String summaryAssessment;  // one-paragraph overall assessment

    // ── Suggested improvements ─────────────────────────────────────────────
    private String improvedTitle;
    private String improvedDescription;
    private List<String> improvedAcceptanceCriteria;
    private Integer suggestedStoryPoints;
    private String storyPointRationale;

    // ── Gap analysis ───────────────────────────────────────────────────────
    private List<EnhancementIssue> issues;

    // ── Epic context insights ───────────────────────────────────────────────
    private String epicContextInsight;  // how this story fits the epic
    private List<String> missingScenarios; // scenarios not covered by current AC

    /** AI disclaimer — always present */
    private String disclaimer;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EnhancementIssue {
        private String severity;   // CRITICAL | ERROR | WARNING | SUGGESTION
        private String field;
        private String problem;
        private String suggestion;
    }
}
