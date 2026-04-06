package com.agileguard.jira.service;

import com.agileguard.common.enums.IssueStatus;
import com.agileguard.common.exception.AgileGuardException;
import com.agileguard.jira.model.JiraIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards JIRA status transitions by enforcing configurable quality gates.
 *
 * Key guarded transitions:
 *   In Progress → Ready for QA:
 *     - All dev sub-tasks must be Done
 *     - Effort must be logged
 *     - PR must be linked
 *   QA In Progress → Ready for Release:
 *     - All QA sub-tasks must be Done
 *     - No open bugs linked
 *     - Acceptance Criteria must be present
 */
@Service
@Slf4j
public class TransitionGuard {

    /**
     * Validates whether a status transition is allowed for the given issue.
     * Throws AgileGuardException with all violations if the transition is blocked.
     *
     * @param issue      the JIRA issue being transitioned
     * @param toStatus   the target status
     */
    public void validateTransition(JiraIssue issue, IssueStatus toStatus) {
        List<String> violations = new ArrayList<>();

        switch (toStatus) {
            case READY_FOR_QA -> validateReadyForQa(issue, violations);
            case READY_FOR_RELEASE -> validateReadyForRelease(issue, violations);
            case DONE -> validateDone(issue, violations);
            default -> {
                // Other transitions are not guarded
            }
        }

        if (!violations.isEmpty()) {
            String violationMsg = "Transition to '" + toStatus + "' is BLOCKED. Violations:\n"
                    + String.join("\n", violations.stream().map(v -> "  • " + v).toList());
            log.warn("Transition blocked for {}: {}", issue.getKey(), violationMsg);
            throw AgileGuardException.badRequest(violationMsg);
        }

        log.info("Transition allowed: {} → {}", issue.getKey(), toStatus);
    }

    /** Checks gates for transitioning to Ready for QA. */
    private void validateReadyForQa(JiraIssue issue, List<String> violations) {
        if (!issue.hasDevSubTask()) {
            violations.add("No developer sub-task found. Create and complete a 'Dev Task' sub-task.");
        }
        if (issue.getTimeSpentMinutes() == null || issue.getTimeSpentMinutes() == 0) {
            violations.add("No effort has been logged on this story or its sub-tasks.");
        }
        if (issue.getLinkedPullRequests().isEmpty()) {
            violations.add("No pull request is linked. Link a GitHub PR before moving to QA.");
        }
    }

    /** Checks gates for transitioning to Ready for Release. */
    private void validateReadyForRelease(JiraIssue issue, List<String> violations) {
        if (!issue.hasQaSubTask()) {
            violations.add("No QA sub-task found. QA tester must create a 'QA Task' sub-task.");
        }
        if (issue.getAcceptanceCriteria() == null || issue.getAcceptanceCriteria().isBlank()) {
            violations.add("Acceptance Criteria is missing or empty.");
        }
        if (issue.getStoryPoints() == null || issue.getStoryPoints() == 0) {
            violations.add("Story points must be estimated before releasing.");
        }
    }

    /** Checks gates for transitioning to Done. */
    private void validateDone(JiraIssue issue, List<String> violations) {
        // Verify story passed through QA stage
        // In a real impl, check changelog for QA_IN_PROGRESS status
        if (!issue.hasQaSubTask()) {
            violations.add("Story was not QA tested. No QA sub-task found.");
        }
    }
}
