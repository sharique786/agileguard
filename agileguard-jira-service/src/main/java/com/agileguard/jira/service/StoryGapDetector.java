package com.agileguard.jira.service;

import com.agileguard.common.enums.GapType;
import com.agileguard.common.enums.IssueStatus;
import com.agileguard.common.enums.IssueType;
import com.agileguard.common.enums.Severity;
import com.agileguard.jira.model.GapFinding;
import com.agileguard.jira.model.GapReport;
import com.agileguard.jira.model.JiraIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Core gap detection engine for AgileGuard.
 *
 * Evaluates a single JIRA issue against all configured quality rules and
 * returns a GapReport with findings and a quality score (0-100).
 *
 * Rules checked:
 *   1. Missing description
 *   2. Missing or improperly formatted acceptance criteria
 *   3. Missing story points (for Stories and Bugs)
 *   4. Missing developer sub-task
 *   5. Missing QA sub-task
 *   6. No effort logged while In Progress
 *   7. Aging stories (configurable threshold)
 *   8. Missing PR link when in review stages
 *
 * Quality score calculation:
 *   Start at 100, deduct per finding:
 *   CRITICAL: -30, ERROR: -15, WARNING: -7
 *   Minimum score is 0.
 */
@Service
@Slf4j
public class StoryGapDetector {

    @Value("${agileguard.jira.aging-threshold-days:5}")
    private int agingThresholdDays;

    @Value("${agileguard.jira.min-description-length:20}")
    private int minDescriptionLength;

    /**
     * Evaluates a JIRA issue and returns a complete GapReport.
     *
     * @param issue the JIRA issue to evaluate
     * @return GapReport with findings and quality score
     */
    public GapReport evaluate(JiraIssue issue) {
        List<GapFinding> findings = new ArrayList<>();

        // Only evaluate stories, tasks, and bugs
        if (issue.getIssueType() == IssueType.EPIC || issue.getIssueType() == IssueType.SUB_TASK) {
            return buildReport(issue, findings);
        }

        // ── Rule 1: Description ───────────────────────────────────────────────
        if (!StringUtils.hasText(issue.getDescription()) ||
            issue.getDescription().trim().length() < minDescriptionLength) {
            findings.add(GapFinding.error(issue.getKey(), GapType.MISSING_DESCRIPTION,
                    "Story is missing a meaningful description (< " + minDescriptionLength + " chars)",
                    "Add a description explaining WHAT this feature does and WHY it is needed"));
        }

        // ── Rule 2: Acceptance Criteria ───────────────────────────────────────
        if (!StringUtils.hasText(issue.getAcceptanceCriteria())) {
            findings.add(GapFinding.error(issue.getKey(), GapType.MISSING_ACCEPTANCE_CRITERIA,
                    "Acceptance Criteria is missing",
                    "Add Acceptance Criteria using Given/When/Then or bullet-point format"));
        } else if (!isValidAcFormat(issue.getAcceptanceCriteria())) {
            findings.add(GapFinding.warning(issue.getKey(), GapType.INVALID_AC_FORMAT,
                    "Acceptance Criteria does not follow Given/When/Then format",
                    "Rewrite AC using: 'Given [context] When [action] Then [outcome]'"));
        }

        // ── Rule 3: Story Points ──────────────────────────────────────────────
        if (issue.getIssueType() == IssueType.STORY || issue.getIssueType() == IssueType.BUG) {
            if (issue.getStoryPoints() == null || issue.getStoryPoints() == 0) {
                findings.add(GapFinding.warning(issue.getKey(), GapType.MISSING_STORY_POINTS,
                        "Story points have not been estimated",
                        "Estimate story points using Fibonacci scale (1, 2, 3, 5, 8, 13)"));
            }
        }

        // ── Rule 4: Developer sub-task ────────────────────────────────────────
        boolean isActivelyWorked = issue.getStatus() == IssueStatus.IN_PROGRESS
                || issue.getStatus() == IssueStatus.IN_REVIEW
                || issue.getStatus() == IssueStatus.READY_FOR_QA;

        if (isActivelyWorked && !issue.hasDevSubTask()) {
            findings.add(GapFinding.warning(issue.getKey(), GapType.MISSING_DEV_SUBTASK,
                    "No developer sub-task found for a story that is In Progress",
                    "Developer should create a 'Dev Task' sub-task and log effort against it"));
        }

        // ── Rule 5: QA sub-task ───────────────────────────────────────────────
        boolean isInQaOrLater = issue.getStatus() == IssueStatus.READY_FOR_QA
                || issue.getStatus() == IssueStatus.QA_IN_PROGRESS
                || issue.getStatus() == IssueStatus.READY_FOR_RELEASE;

        if (isInQaOrLater && !issue.hasQaSubTask()) {
            findings.add(GapFinding.warning(issue.getKey(), GapType.MISSING_QA_SUBTASK,
                    "No QA sub-task found for a story in the QA pipeline",
                    "QA Tester should create a 'QA Task' sub-task and log effort"));
        }

        // ── Rule 6: Effort logging ────────────────────────────────────────────
        if (issue.getStatus() == IssueStatus.IN_PROGRESS) {
            if (issue.getTimeSpentMinutes() == null || issue.getTimeSpentMinutes() == 0) {
                findings.add(GapFinding.warning(issue.getKey(), GapType.NO_EFFORT_LOGGED,
                        "Story is In Progress but no effort has been logged",
                        "Log effort daily using the JIRA worklog or the sub-task"));
            }
        }

        // ── Rule 7: Aging stories ─────────────────────────────────────────────
        long daysInStatus = issue.getDaysInCurrentStatus();
        if (daysInStatus > agingThresholdDays && issue.getStatus() != IssueStatus.DONE) {
            String msg = String.format("Story has been in '%s' for %d days (threshold: %d)",
                    issue.getStatus(), daysInStatus, agingThresholdDays);
            findings.add(GapFinding.critical(issue.getKey(), GapType.AGING_STORY, msg,
                    "Review this story in the next standup. Check for blockers."));
        }

        // ── Rule 8: PR link ───────────────────────────────────────────────────
        if (issue.getStatus() == IssueStatus.IN_REVIEW
                && issue.getLinkedPullRequests().isEmpty()) {
            findings.add(GapFinding.warning(issue.getKey(), GapType.MISSING_PR_LINK,
                    "Story is In Review but no pull request is linked",
                    "Link the GitHub PR to this JIRA issue via the development panel"));
        }

        return buildReport(issue, findings);
    }

    /**
     * Validates that acceptance criteria follows a structured format.
     * Accepts Given/When/Then format or bullet-point lists with at least 2 items.
     */
    private boolean isValidAcFormat(String ac) {
        if (ac == null) return false;
        String lower = ac.toLowerCase();
        boolean hasGivenWhenThen = lower.contains("given") && lower.contains("when")
                && lower.contains("then");
        boolean hasBulletPoints = ac.contains("- ") || ac.contains("* ") || ac.contains("1.");
        return hasGivenWhenThen || hasBulletPoints;
    }

    /** Builds the GapReport from the collected findings and calculates quality score. */
    private GapReport buildReport(JiraIssue issue, List<GapFinding> findings) {
        int criticalCount = (int) findings.stream()
                .filter(f -> f.getSeverity() == Severity.CRITICAL).count();
        int errorCount = (int) findings.stream()
                .filter(f -> f.getSeverity() == Severity.ERROR).count();
        int warningCount = (int) findings.stream()
                .filter(f -> f.getSeverity() == Severity.WARNING).count();

        int score = 100 - (criticalCount * 30) - (errorCount * 15) - (warningCount * 7);
        score = Math.max(0, score);

        boolean flagged = score < 40 || criticalCount > 0;

        log.debug("Evaluated {}: score={}, findings={}, flagged={}", issue.getKey(), score,
                findings.size(), flagged);

        return GapReport.builder()
                .issueKey(issue.getKey())
                .issueSummary(issue.getSummary())
                .qualityScore(score)
                .flagged(flagged)
                .findings(findings)
                .criticalCount(criticalCount)
                .errorCount(errorCount)
                .warningCount(warningCount)
                .build();
    }
}
