package com.agileguard.ai.controller;

import com.agileguard.ai.model.EpicContextRequest;
import com.agileguard.ai.model.EpicGenerationRequest;
import com.agileguard.ai.model.EpicGenerationResponse;
import com.agileguard.ai.model.StoryEnhancementRequest;
import com.agileguard.ai.model.StoryEnhancementResponse;
import com.agileguard.ai.model.StoryValidationRequest;
import com.agileguard.ai.model.ValidationResult;
import com.agileguard.ai.service.EpicStoryGenerationService;
import com.agileguard.ai.service.StoryValidationService;
import com.agileguard.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST controller for AI-powered story creation and validation.
 *
 * Existing endpoints (unchanged):
 *   POST /api/ai/validate               — full story quality validation
 *   POST /api/ai/ac/generate            — generate Given/When/Then AC
 *   GET  /api/ai/validate/stream        — SSE real-time validation
 *
 * New endpoints for enhanced Story Editor modes:
 *   POST /api/ai/epic/generate          — Mode A: epic → stories
 *   POST /api/ai/story/suggest-ac       — Mode B: new story AC from epic context
 *   POST /api/ai/story/suggest-enhancements — Mode C: update story suggestions
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final StoryValidationService     validationService;
    private final EpicStoryGenerationService epicService;

    // ── Existing endpoints ─────────────────────────────────────────────────

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<ValidationResult>> validate(
            @Valid @RequestBody StoryValidationRequest request) {
        ValidationResult result = validationService.validate(request);
        return ResponseEntity.ok(ApiResponse.success("Validation complete", result));
    }

    @PostMapping("/ac/generate")
    public ResponseEntity<ApiResponse<List<String>>> generateAC(
            @RequestBody Map<String, String> body) {
        String title       = body.getOrDefault("title", "");
        String description = body.getOrDefault("description", "");
        List<String> criteria = validationService.generateAcceptanceCriteria(title, description);
        return ResponseEntity.ok(ApiResponse.success("AC generated", criteria));
    }

    @GetMapping(value = "/validate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamValidation(
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam(required = false, defaultValue = "") String ac) {

        StoryValidationRequest request = new StoryValidationRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setAcceptanceCriteria(ac);
        ValidationResult result = validationService.validate(request);

        String scoreChunk   = "{\"type\":\"score\",\"value\":"   + result.getQualityScore() + "}";
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
        String disclaimerChunk = "{\"type\":\"disclaimer\",\"value\":\"" +
                EpicStoryGenerationService.DISCLAIMER.replace("\"", "'") + "\"}";
        String doneChunk = "{\"type\":\"done\"}";

        return Flux.just(scoreChunk, summaryChunk, issuesChunk, disclaimerChunk, doneChunk)
                .delayElements(Duration.ofMillis(200));
    }

    // ── New endpoints ──────────────────────────────────────────────────────

    /**
     * Mode A — Create Epic:
     * Decomposes an epic description into fully-formed JIRA stories.
     * Considers team size, bandwidth, and all AgileGuard-required fields.
     */
    @PostMapping("/epic/generate")
    public ResponseEntity<ApiResponse<EpicGenerationResponse>> generateStoriesFromEpic(
            @Valid @RequestBody EpicGenerationRequest request) {
        log.info("Epic generation request: '{}' | {} devs", request.getEpicTitle(),
                request.getNumberOfDevelopers());
        EpicGenerationResponse response = epicService.generateStoriesFromEpic(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Stories generated from epic. " + EpicStoryGenerationService.DISCLAIMER, response));
    }

    /**
     * Mode B — Create Story (epic context):
     * Returns AC suggestions for a new story based on its parent epic.
     * Avoids duplicating scenarios covered by existing epic stories.
     */
    @PostMapping("/story/suggest-ac")
    public ResponseEntity<ApiResponse<List<String>>> suggestAcFromEpicContext(
            @RequestBody EpicContextRequest request) {
        List<String> suggestions = epicService.suggestAcFromEpicContext(request);
        return ResponseEntity.ok(ApiResponse.success("AC suggested from epic context", suggestions));
    }

    /**
     * Mode C — Update Story:
     * Analyses the existing story and returns enhancement suggestions per field.
     * Uses epic context and current gap findings for richer suggestions.
     */
    @PostMapping("/story/suggest-enhancements")
    public ResponseEntity<ApiResponse<StoryEnhancementResponse>> suggestEnhancements(
            @RequestBody StoryEnhancementRequest request) {
        StoryEnhancementResponse response = epicService.suggestEnhancements(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Enhancement suggestions ready. " + EpicStoryGenerationService.DISCLAIMER, response));
    }
}
