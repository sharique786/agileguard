package com.agileguard.ai.controller;

import com.agileguard.ai.model.StoryValidationRequest;
import com.agileguard.ai.model.ValidationResult;
import com.agileguard.ai.service.StoryValidationService;
import com.agileguard.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST controller for AI-powered story validation and suggestion endpoints.
 * Provides both synchronous validation and SSE streaming for real-time suggestions.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final StoryValidationService validationService;

    /**
     * Validates a complete story draft and returns full AI analysis.
     * Called when user saves or explicitly requests AI review.
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<ValidationResult>> validate(
            @Valid @RequestBody StoryValidationRequest request) {
        ValidationResult result = validationService.validate(request);
        return ResponseEntity.ok(ApiResponse.success("Validation complete", result));
    }

    /**
     * Generates Acceptance Criteria suggestions for a story.
     * Returns a list of Given/When/Then criteria.
     */
    @PostMapping("/ac/generate")
    public ResponseEntity<ApiResponse<List<String>>> generateAC(
            @RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        String description = body.getOrDefault("description", "");
        List<String> criteria = validationService.generateAcceptanceCriteria(title, description);
        return ResponseEntity.ok(ApiResponse.success("AC generated", criteria));
    }

    /**
     * Server-Sent Events endpoint that streams validation feedback in real time.
     * Called by Angular on debounced keystrokes (800ms) in the story form.
     * Returns a stream of JSON chunks as the AI processes the story.
     */
    @GetMapping(value = "/validate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamValidation(
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam(required = false, defaultValue = "") String ac) {

        log.debug("SSE stream requested for story: '{}'", title);

        // Build request and get validation result
        StoryValidationRequest request = new StoryValidationRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setAcceptanceCriteria(ac);

        ValidationResult result = validationService.validate(request);

        // Stream the result as SSE chunks (simulate streaming for POC)
        String scoreChunk = "{\"type\":\"score\",\"value\":" + result.getQualityScore() + "}";
        String summaryChunk = "{\"type\":\"summary\",\"value\":\"" +
                result.getSummary().replace("\"", "'") + "\"}";
        String issuesChunk;
        try {
            issuesChunk = "{\"type\":\"issues\",\"value\":" +
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(result.getIssues()) + "}";
        } catch (Exception e) {
            issuesChunk = "{\"type\":\"issues\",\"value\":[]}";
        }
        String doneChunk = "{\"type\":\"done\"}";

        return Flux.just(scoreChunk, summaryChunk, issuesChunk, doneChunk)
                .delayElements(Duration.ofMillis(200));
    }
}
