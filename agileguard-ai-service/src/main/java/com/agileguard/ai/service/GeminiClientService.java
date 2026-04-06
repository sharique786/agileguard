package com.agileguard.ai.service;

import com.agileguard.ai.model.GeminiRequest;
import com.agileguard.ai.model.GeminiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * HTTP client for the Google Gemini generateContent REST API.
 *
 * Configure your Gemini API key in application.yml (agileguard.gemini.api-key).
 * When use-mock=true (default for local dev), returns realistic mock responses
 * without making any actual API calls.
 *
 * Gemini API endpoint:
 *   POST https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={apiKey}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiClientService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${agileguard.gemini.api-key:#{null}}")
    private String apiKey;

    @Value("${agileguard.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${agileguard.gemini.use-mock:true}")
    private boolean useMock;

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    /**
     * Sends a text prompt to Gemini and returns the text response.
     *
     * @param prompt the prompt text to send
     * @return Gemini's text response
     */
    public String generateContent(String prompt) {
        if (useMock) {
            log.info("Using mock Gemini response (set agileguard.gemini.use-mock=false for real AI)");
            return getMockResponse(prompt);
        }

        try {
            GeminiRequest request = GeminiRequest.builder()
                    .contents(List.of(GeminiRequest.Content.builder()
                            .parts(List.of(GeminiRequest.Part.builder().text(prompt).build()))
                            .build()))
                    .generationConfig(GeminiRequest.GenerationConfig.builder()
                            .temperature(0.3)
                            .maxOutputTokens(1024)
                            .topP(0.8)
                            .build())
                    .build();

            String url = BASE_URL + model + ":generateContent?key=" + apiKey;

            GeminiResponse response = webClientBuilder.build()
                    .post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), GeminiRequest.class)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            String text = response != null ? response.extractText() : "";
            log.debug("Gemini response: {} chars", text.length());
            return text;

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return getMockResponse(prompt);  // fallback to mock on error
        }
    }

    /**
     * Provides realistic mock responses for local development.
     * Detects whether the prompt is for validation or AC generation.
     */
    private String getMockResponse(String prompt) {
        if (prompt.contains("Acceptance Criteria") && prompt.contains("Generate")) {
            return """
                    ["Given a user with valid credentials, When they submit the login form, Then they are authenticated and redirected to the dashboard.",
                     "Given an invalid password, When the user submits the form, Then an error message is displayed and the form is not submitted.",
                     "Given a user on mobile, When they log in, Then the session persists for 24 hours.",
                     "Given a user who forgets their password, When they click 'Forgot Password', Then a reset email is sent within 2 minutes."]
                    """;
        }

        // Validation mock response
        return """
                {
                  "qualityScore": 72,
                  "summary": "The story has a good description but the acceptance criteria needs improvement with structured Given/When/Then format.",
                  "issues": [
                    {"severity": "WARNING", "field": "acceptanceCriteria",
                     "message": "AC is present but not in structured Given/When/Then format. Consider rewriting for clarity."},
                    {"severity": "INFO", "field": "storyPoints",
                     "message": "Story points look reasonable. Consider breaking down stories > 8 points."}
                  ],
                  "suggestions": {
                    "improvedDescription": "As a registered user, I want to be able to log in to the platform so that I can access my personalised dashboard and manage my account settings.",
                    "acceptanceCriteria": [
                      "Given a user with valid email and password, When they submit the login form, Then they are authenticated and redirected to the dashboard within 3 seconds.",
                      "Given a user with an incorrect password, When they submit the form, Then an error message 'Invalid credentials' is shown and the account is not locked on first attempt.",
                      "Given a user who has been inactive for 30 minutes, When they attempt an action, Then they are automatically redirected to the login page.",
                      "Given a user on any device, When they log in, Then the session is secured with HTTPS and a CSRF token."
                    ],
                    "estimationHint": "This story appears to be a 3-5 point effort based on the scope described."
                  }
                }
                """;
    }
}
