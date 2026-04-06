package com.agileguard.ai.service;

import com.agileguard.ai.model.StoryValidationRequest;
import com.agileguard.ai.model.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StoryValidationService.
 * Mocks Gemini client to test prompt building and response parsing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoryValidationService Tests")
class StoryValidationServiceTest {

    @InjectMocks
    private StoryValidationService service;

    @Mock
    private GeminiClientService geminiClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private StoryValidationRequest buildRequest(String title, String desc, String ac) {
        StoryValidationRequest req = new StoryValidationRequest();
        req.setTitle(title);
        req.setDescription(desc);
        req.setAcceptanceCriteria(ac);
        req.setIssueType("Story");
        req.setStoryPoints(5);
        return req;
    }

    @Test
    @DisplayName("validate — Should parse valid JSON response and return high score")
    void validate_validJsonResponse_returnsHighScore() {
        String mockJson = """
                {"qualityScore":85,"summary":"Well written story.","issues":[],
                 "suggestions":{"improvedDescription":"As a user...","acceptanceCriteria":["Given..."],
                 "estimationHint":"5 points"}}
                """;
        when(geminiClient.generateContent(anyString())).thenReturn(mockJson);

        ValidationResult result = service.validate(buildRequest(
                "User login", "As a user I want to login", "Given...When...Then..."));

        assertThat(result.getQualityScore()).isEqualTo(85);
        assertThat(result.getSummary()).isEqualTo("Well written story.");
        assertThat(result.getIssues()).isEmpty();
        assertThat(result.isValid()).isTrue(); // 85 >= 60
    }

    @Test
    @DisplayName("validate — Should return isValid=false when score < 60")
    void validate_lowScore_isValidFalse() {
        String mockJson = """
                {"qualityScore":45,"summary":"Many gaps found.","issues":[
                  {"severity":"ERROR","field":"description","message":"Missing description"}
                ],"suggestions":{"improvedDescription":"...","acceptanceCriteria":[],"estimationHint":""}}
                """;
        when(geminiClient.generateContent(anyString())).thenReturn(mockJson);

        ValidationResult result = service.validate(buildRequest("Fix bug", "", ""));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getSeverity()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("validate — Should handle malformed JSON gracefully")
    void validate_malformedJson_returnsFallback() {
        when(geminiClient.generateContent(anyString())).thenReturn("not valid json {{{}");

        ValidationResult result = service.validate(buildRequest("Title", "Desc", "AC"));

        assertThat(result).isNotNull();
        assertThat(result.getQualityScore()).isEqualTo(50);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("validate — Should strip markdown fences from Gemini response")
    void validate_markdownFencedJson_parsedCorrectly() {
        String fenced = "```json\n{\"qualityScore\":90,\"summary\":\"Great.\",\"issues\":[],\"suggestions\":{\"improvedDescription\":\"\",\"acceptanceCriteria\":[],\"estimationHint\":\"\"}}\n```";
        when(geminiClient.generateContent(anyString())).thenReturn(fenced);

        ValidationResult result = service.validate(buildRequest("T", "D", "AC"));

        assertThat(result.getQualityScore()).isEqualTo(90);
    }

    @Test
    @DisplayName("generateAC — Should return a list of AC strings")
    void generateAC_returnsListOfCriteria() {
        String mockAcJson = """
                ["Given valid credentials, When login submitted, Then authenticated.",
                 "Given wrong password, When login submitted, Then error shown."]
                """;
        when(geminiClient.generateContent(anyString())).thenReturn(mockAcJson);

        List<String> criteria = service.generateAcceptanceCriteria("Login", "User wants to login");

        assertThat(criteria).hasSize(2);
        assertThat(criteria.get(0)).contains("Given");
    }

    @Test
    @DisplayName("buildValidationPrompt — Should include all story fields in prompt")
    void buildValidationPrompt_includesAllFields() {
        StoryValidationRequest req = buildRequest("Login story", "As a user...", "Given...Then...");

        String prompt = service.buildValidationPrompt(req);

        assertThat(prompt).contains("Login story");
        assertThat(prompt).contains("As a user...");
        assertThat(prompt).contains("Given...Then...");
        assertThat(prompt).contains("Story");
        assertThat(prompt).contains("qualityScore");
    }
}
