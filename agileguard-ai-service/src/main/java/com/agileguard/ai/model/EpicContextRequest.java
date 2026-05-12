package com.agileguard.ai.model;

import lombok.Data;

import java.util.List;

/**
 * Request to get AC suggestions for a NEW story based on its parent epic context.
 * Called when user fills in an epic link on the Create Story form.
 */
@Data
public class EpicContextRequest {

    /** Epic issue key (e.g., "COMMSSURV-10") */
    private String epicKey;
    private String epicName;
    private String epicDescription;   // fetched from JIRA

    /** The new story being written */
    private String storyTitle;
    private String storyDescription;  // may be empty at this point
    private String businessLine;
    private String issueType;

    /** Existing stories in the epic so AI avoids duplicate AC */
    private List<String> existingStorySummaries;
}
