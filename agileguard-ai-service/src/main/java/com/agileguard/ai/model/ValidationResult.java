package com.agileguard.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of AI-powered story validation from Gemini.
 * Contains a quality score, detected issues, and suggestions.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ValidationResult {

    private int qualityScore;          // 0-100
    private String summary;            // One-sentence overall assessment
    private List<ValidationIssue> issues;
    private AiSuggestions suggestions;
    private boolean valid;
    private String rawAiResponse;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ValidationIssue {
        private String severity;       // ERROR, WARNING, INFO
        private String field;
        private String message;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AiSuggestions {
        private String improvedDescription;
        private List<String> acceptanceCriteria;
        private String estimationHint;
    }
}
