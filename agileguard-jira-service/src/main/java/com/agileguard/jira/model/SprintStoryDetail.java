package com.agileguard.jira.model;

import com.agileguard.common.enums.IssueStatus;
import com.agileguard.common.enums.IssueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single story/issue as it appears in the SAFe sprint report.
 * Captures the snapshot of a story's state at sprint end.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SprintStoryDetail {

    private String      issueKey;         // "PLAT-101"
    private String      summary;          // story title
    private IssueType   issueType;        // STORY | BUG | TASK
    private IssueStatus status;           // final status at report generation
    private Integer     storyPoints;      // points committed for this story
    private String      assigneeName;
    private String      epicLink;         // parent epic key
    private String      epicName;         // resolved epic name
    private String      priority;
    private String      businessLine;

    /**
     * Category in the sprint report:
     *   COMPLETED  — story reached DONE during this sprint
     *   SPILLED    — was in sprint but not completed (carried forward)
     *   ADDED      — added to sprint after sprint start (scope creep)
     *   REMOVED    — removed from sprint before sprint end
     */
    private String  reportCategory;

    private String  spillReason;          // free-text reason if spilled (from JIRA comment / label)
    private String  movedToSprint;        // sprint name it was moved to, if known
    private Integer timeSpentMinutes;     // actual effort logged
    private boolean hasDevSubTask;
    private boolean hasQaSubTask;
}
