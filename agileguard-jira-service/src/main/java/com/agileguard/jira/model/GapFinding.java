package com.agileguard.jira.model;

import com.agileguard.common.enums.GapType;
import com.agileguard.common.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A single gap finding detected by the StoryGapDetector.
 * Multiple findings can exist for one JIRA issue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapFinding {

    private String issueKey;
    private GapType type;
    private Severity severity;
    private String message;
    private String suggestedAction;

    @Builder.Default
    private Instant detectedAt = Instant.now();

    // Factory methods for common severities

    /** Creates a CRITICAL severity finding. */
    public static GapFinding critical(String issueKey, GapType type, String message, String action) {
        return GapFinding.builder()
                .issueKey(issueKey).type(type).severity(Severity.CRITICAL)
                .message(message).suggestedAction(action).build();
    }

    /** Creates an ERROR severity finding. */
    public static GapFinding error(String issueKey, GapType type, String message, String action) {
        return GapFinding.builder()
                .issueKey(issueKey).type(type).severity(Severity.ERROR)
                .message(message).suggestedAction(action).build();
    }

    /** Creates a WARNING severity finding. */
    public static GapFinding warning(String issueKey, GapType type, String message, String action) {
        return GapFinding.builder()
                .issueKey(issueKey).type(type).severity(Severity.WARNING)
                .message(message).suggestedAction(action).build();
    }
}
