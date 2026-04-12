package com.agileguard.ai.model;

import lombok.Data;

import java.util.List;

/**
 * Request for AI enhancement suggestions on an existing story.
 * Used in "Update Story" mode — current story content is sent for review.
 */
@Data
public class StoryEnhancementRequest {

    /** JIRA issue key of the existing story (e.g., "PLAT-123") */
    private String issueKey;

    private String title;
    private String description;
    private String acceptanceCriteria;
    private String issueType;
    private String status;
    private Integer storyPoints;
    private String priority;
    private String businessLine;

    /** Epic link — used to fetch epic context for richer suggestions */
    private String epicLink;
    private String epicName;
    private String epicDescription;    // fetched from JIRA if available

    private List<String> components;
    private List<String> labels;
    private List<String> teamNames;
    private List<String> fixVersions;

    /** Current gap findings from the gap detection engine */
    private List<String> existingGapMessages;
}
