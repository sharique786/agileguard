package com.agileguard.ai.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request payload for story validation. */
@Data
public class StoryValidationRequest {
    @NotBlank(message = "Story title is required")
    private String title;
    private String description;
    private String acceptanceCriteria;
    private String issueType;
    private Integer storyPoints;
    private String tenantId;
}
