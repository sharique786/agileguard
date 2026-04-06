package com.agileguard.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/** Response body from the Google Gemini generateContent API. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    private List<Candidate> candidates;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;

        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Content {
            private List<Part> parts;

            @Data @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Part {
                private String text;
            }
        }
    }

    /** Extracts the text from the first candidate's first part. */
    public String extractText() {
        if (candidates == null || candidates.isEmpty()) return "";
        var parts = candidates.get(0).getContent().getParts();
        if (parts == null || parts.isEmpty()) return "";
        return parts.get(0).getText();
    }
}
