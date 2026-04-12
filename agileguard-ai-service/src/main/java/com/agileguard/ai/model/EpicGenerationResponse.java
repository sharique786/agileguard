package com.agileguard.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from AI epic decomposition.
 * Contains all generated stories plus summary analysis.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EpicGenerationResponse {

    private String epicTitle;
    private String epicKey;

    /** AI's analysis of the epic scope */
    private String epicAnalysis;

    /** Total story points estimated for the epic */
    private int totalEstimatedPoints;

    /** Estimated number of sprints to complete the epic */
    private int estimatedSprints;

    /** Number of developer stories generated */
    private int developerStoryCount;

    /** Number of QA/testing stories generated */
    private int qaStoryCount;

    /** All generated stories ready for JIRA submission */
    private List<GeneratedStory> stories;

    /** AI recommendations about team structure or epic scope */
    private String recommendations;

    /** AI disclaimer — always present */
    private String disclaimer;
}
