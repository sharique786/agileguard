package com.agileguard.jira.model;

import com.agileguard.common.enums.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A JIRA sub-task attached to a story — e.g. "Dev Task" or "QA Task".
 *
 * Consumers:
 *   - JiraIssue.subTasks              (declares the field, this backs it)
 *   - StoryGapDetector.hasSubTaskOfType()   — rules 5/6 (missing dev/QA sub-task)
 *   - WorkflowService.hasClosedSubtaskOfType() — release-readiness Gate 2
 *
 * `type` is matched case-insensitively against keywords like "Dev Task",
 * "Development", "QA Task", "Testing" — it is NOT a JIRA issue-type enum
 * because sub-task type naming varies per JIRA project configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {

    /** Sub-task's own JIRA key, e.g. "PLAT-102-1". */
    private String key;

    /** Free-text sub-task type/category, e.g. "Dev Task", "QA Task". */
    private String type;

    /** Sub-task's current status. Must be DONE for release-readiness gates to pass. */
    private IssueStatus status;

    private String summary;
}
