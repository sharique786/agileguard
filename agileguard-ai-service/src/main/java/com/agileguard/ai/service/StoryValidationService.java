package com.agileguard.ai.service;

import com.agileguard.ai.model.StoryValidationRequest;
import com.agileguard.ai.model.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates AI-powered story validation by building structured prompts,
 * calling Gemini, and parsing the JSON response into a ValidationResult.
 *
 * The prompt instructs Gemini to act as an Agile Quality Coach and return
 * a structured JSON response covering: quality score, issues, and suggestions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoryValidationService {

    private final GeminiClientService geminiClient;
    private final ObjectMapper objectMapper;

    /**
     * Validates a story draft against Agile best practices using Gemini AI.
     *
     * @param request story details to validate
     * @return ValidationResult with score, issues, and AI suggestions
     */
    public ValidationResult validate(StoryValidationRequest request) {
        String prompt = buildValidationPrompt(request);
        log.info("Validating story: '{}' via Gemini AI", request.getTitle());

        String rawResponse = geminiClient.generateContent(prompt);
        return parseValidationResponse(rawResponse);
    }

    /**
     * Generates structured Acceptance Criteria for a story using Gemini AI.
     *
     * @param title       story title
     * @param description story description
     * @return list of AC strings in Given/When/Then format
     */
    public List<String> generateAcceptanceCriteria(String title, String description) {
        String prompt = buildAcGenerationPrompt(title, description);
        log.info("Generating AC for story: '{}'", title);

        String rawResponse = geminiClient.generateContent(prompt);
        return parseAcResponse(rawResponse);
    }

    /**
     * Builds a structured prompt for story validation.
     * Instructs Gemini to return a strict JSON object — no markdown, no preamble.
     */
    String buildValidationPrompt(StoryValidationRequest req) {
        return """
                You are an expert Agile Quality Coach reviewing JIRA user stories.
                Evaluate the following story and return ONLY a valid JSON object.
                Do NOT include any markdown, code fences, or explanatory text outside the JSON.
                
                Story Details:
                Title: %s
                Description: %s
                Acceptance Criteria: %s
                Issue Type: %s
                Story Points: %s
                
                Return exactly this JSON structure:
                {
                  "qualityScore": <integer 0-100>,
                  "summary": "<one sentence overall assessment>",
                  "issues": [
                    {"severity": "<ERROR|WARNING|INFO>", "field": "<fieldName>", "message": "<issue message>"}
                  ],
                  "suggestions": {
                    "improvedDescription": "<rewritten description as a user story>",
                    "acceptanceCriteria": ["<criterion 1 in Given/When/Then>", "<criterion 2>"],
                    "estimationHint": "<story point advice>"
                  }
                }
                
                Scoring rules:
                - Start at 100, deduct for each issue found
                - Missing description: -25
                - Missing or vague AC: -25
                - AC not in Given/When/Then or bullet format: -10
                - Missing story points: -10
                - Description not in user-story format: -10
                - AC items not testable: -10 each
                """.formatted(
                req.getTitle(),
                orEmpty(req.getDescription()),
                orEmpty(req.getAcceptanceCriteria()),
                orEmpty(req.getIssueType()),
                req.getStoryPoints() != null ? req.getStoryPoints() : "Not estimated"
        );
    }

    /**
     * Builds a prompt to generate Acceptance Criteria.
     * Returns a JSON array of Given/When/Then strings.
     */
    private String buildAcGenerationPrompt(String title, String description) {
        return """
                Generate 3-5 Acceptance Criteria in Given/When/Then format for this user story.
                Return ONLY a JSON array of strings. No markdown, no extra text.
                
                Story Title: %s
                Story Description: %s
                
                Each criterion must be:
                - Testable and unambiguous
                - In format: "Given [context], When [action], Then [outcome]"
                - Cover happy path, error cases, and edge cases
                
                Return format: ["criterion 1", "criterion 2", ...]
                """.formatted(title, orEmpty(description));
    }

    /** Parses Gemini's JSON response into a ValidationResult. */
    private ValidationResult parseValidationResponse(String raw) {
        try {
            String cleaned = cleanJsonResponse(raw);
            ValidationResult result = objectMapper.readValue(cleaned, ValidationResult.class);
            result.setRawAiResponse(raw);
            result.setValid(result.getQualityScore() >= 60);
            return result;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini validation response: {}", e.getMessage());
            return ValidationResult.builder()
                    .qualityScore(50)
                    .summary("AI validation encountered a parsing error. Please review manually.")
                    .issues(List.of())
                    .valid(false)
                    .rawAiResponse(raw)
                    .build();
        }
    }

    /** Parses Gemini's JSON array response into a list of AC strings. */
    @SuppressWarnings("unchecked")
    private List<String> parseAcResponse(String raw) {
        try {
            String cleaned = cleanJsonResponse(raw);
            return objectMapper.readValue(cleaned, List.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini AC response: {}", e.getMessage());
            return List.of(
                    "Given [context], When [action], Then [expected outcome]",
                    "Given an error condition, When the action fails, Then an appropriate error message is shown"
            );
        }
    }

    /** Strips markdown code fences from Gemini responses. */
    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        return raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
    }

    private String orEmpty(String val) {
        return val != null ? val : "(not provided)";
    }
}
