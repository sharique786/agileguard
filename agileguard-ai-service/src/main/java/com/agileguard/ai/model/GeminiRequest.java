package com.agileguard.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Request body structure for the Google Gemini generateContent API. */
@Data @Builder
public class GeminiRequest {

    private List<Content> contents;
    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;

    @Data @Builder
    public static class Content {
        private List<Part> parts;
    }

    @Data @Builder
    public static class Part {
        private String text;
    }

    @Data @Builder
    public static class GenerationConfig {
        private double temperature;
        @JsonProperty("maxOutputTokens")
        private int maxOutputTokens;
        @JsonProperty("topP")
        private double topP;
    }
}
