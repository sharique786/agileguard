package com.agileguard.jira.service;

import com.agileguard.common.enums.*;
import com.agileguard.jira.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for StoryGapDetector.
 * Tests each gap rule in isolation with clearly named test cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoryGapDetector Tests")
class StoryGapDetectorTest {

    @InjectMocks
    private StoryGapDetector detector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(detector, "agingThresholdDays", 5);
        ReflectionTestUtils.setField(detector, "minDescriptionLength", 20);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private JiraIssue perfectStory() {
        return JiraIssue.builder()
                .key("PROJ-100").summary("Perfect story")
                .description("As a user, I want to log in using SSO so that I can access my account securely.")
                .acceptanceCriteria("Given a user with valid SSO credentials, When they click Login, Then they are authenticated and redirected to dashboard.")
                .issueType(IssueType.STORY).status(IssueStatus.IN_PROGRESS)
                .storyPoints(5).timeSpentMinutes(60)
                .statusChangedAt(LocalDate.now().minusDays(2))
                .subTasks(List.of(
                    JiraSubTask.builder().type("Dev Task").status("In Progress").build()))
                .linkedPullRequests(List.of("PR-99"))
                .build();
    }

    // ── Rule 1: Description ───────────────────────────────────────────────────

    @Test
    @DisplayName("Rule 1 — Should flag ERROR when description is blank")
    void evaluate_blankDescription_flagsError() {
        JiraIssue issue = perfectStory();
        issue.setDescription("");

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.MISSING_DESCRIPTION
                            && f.getSeverity() == Severity.ERROR);
    }

    @Test
    @DisplayName("Rule 1 — Should flag ERROR when description is too short")
    void evaluate_shortDescription_flagsError() {
        JiraIssue issue = perfectStory();
        issue.setDescription("Fix it");

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.MISSING_DESCRIPTION);
    }

    @Test
    @DisplayName("Rule 1 — Should NOT flag when description is adequate")
    void evaluate_goodDescription_noDescriptionError() {
        GapReport report = detector.evaluate(perfectStory());

        assertThat(report.getFindings())
                .noneMatch(f -> f.getType() == GapType.MISSING_DESCRIPTION);
    }

    // ── Rule 2: Acceptance Criteria ───────────────────────────────────────────

    @Test
    @DisplayName("Rule 2 — Should flag ERROR when AC is missing")
    void evaluate_missingAC_flagsError() {
        JiraIssue issue = perfectStory();
        issue.setAcceptanceCriteria(null);

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.MISSING_ACCEPTANCE_CRITERIA
                            && f.getSeverity() == Severity.ERROR);
    }

    @Test
    @DisplayName("Rule 2 — Should flag WARNING for AC without proper format")
    void evaluate_badACFormat_flagsWarning() {
        JiraIssue issue = perfectStory();
        issue.setAcceptanceCriteria("It should work correctly");

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.INVALID_AC_FORMAT
                            && f.getSeverity() == Severity.WARNING);
    }

    @Test
    @DisplayName("Rule 2 — Should accept bullet-point AC as valid format")
    void evaluate_bulletPointAC_noFormatWarning() {
        JiraIssue issue = perfectStory();
        issue.setAcceptanceCriteria("- User can log in\n- Error message shown on failure");

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .noneMatch(f -> f.getType() == GapType.INVALID_AC_FORMAT);
    }

    // ── Rule 3: Story Points ──────────────────────────────────────────────────

    @Test
    @DisplayName("Rule 3 — Should flag WARNING when story points are missing")
    void evaluate_missingStoryPoints_flagsWarning() {
        JiraIssue issue = perfectStory();
        issue.setStoryPoints(null);

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.MISSING_STORY_POINTS
                            && f.getSeverity() == Severity.WARNING);
    }

    @Test
    @DisplayName("Rule 3 — Should NOT flag story points for EPICs")
    void evaluate_epicWithoutPoints_noPointsFlag() {
        JiraIssue issue = perfectStory();
        issue.setIssueType(IssueType.EPIC);
        issue.setStoryPoints(null);

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings()).isEmpty();
    }

    // ── Rule 4: Dev sub-task ──────────────────────────────────────────────────

    @Test
    @DisplayName("Rule 4 — Should flag WARNING when Dev sub-task missing on In Progress story")
    void evaluate_noDevSubTask_inProgress_flagsWarning() {
        JiraIssue issue = perfectStory();
        issue.setSubTasks(List.of()); // no sub-tasks

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.MISSING_DEV_SUBTASK);
    }

    @Test
    @DisplayName("Rule 4 — Should NOT flag Dev sub-task for To Do stories")
    void evaluate_noDevSubTask_toDo_noFlag() {
        JiraIssue issue = perfectStory();
        issue.setStatus(IssueStatus.TO_DO);
        issue.setSubTasks(List.of());

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .noneMatch(f -> f.getType() == GapType.MISSING_DEV_SUBTASK);
    }

    // ── Rule 5: QA sub-task ───────────────────────────────────────────────────

    @Test
    @DisplayName("Rule 5 — Should flag WARNING when QA sub-task missing in QA stage")
    void evaluate_noQaSubTask_readyForQa_flagsWarning() {
        JiraIssue issue = perfectStory();
        issue.setStatus(IssueStatus.READY_FOR_QA);
        issue.setSubTasks(List.of()); // no sub-tasks

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.MISSING_QA_SUBTASK);
    }

    // ── Rule 6: Effort logging ────────────────────────────────────────────────

    @Test
    @DisplayName("Rule 6 — Should flag WARNING when no effort logged on In Progress story")
    void evaluate_noEffortLogged_inProgress_flagsWarning() {
        JiraIssue issue = perfectStory();
        issue.setTimeSpentMinutes(0);

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.NO_EFFORT_LOGGED);
    }

    // ── Rule 7: Aging ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Rule 7 — Should flag CRITICAL for story beyond aging threshold")
    void evaluate_agingStory_flagsCritical() {
        JiraIssue issue = perfectStory();
        issue.setStatusChangedAt(LocalDate.now().minusDays(8)); // 8 days, threshold is 5

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == GapType.AGING_STORY
                            && f.getSeverity() == Severity.CRITICAL);
        assertThat(report.isFlagged()).isTrue();
    }

    @Test
    @DisplayName("Rule 7 — Should NOT flag aging for stories within threshold")
    void evaluate_recentStory_noAgingFlag() {
        JiraIssue issue = perfectStory();
        issue.setStatusChangedAt(LocalDate.now().minusDays(2)); // within threshold

        GapReport report = detector.evaluate(issue);

        assertThat(report.getFindings())
                .noneMatch(f -> f.getType() == GapType.AGING_STORY);
    }

    // ── Quality Score ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Score — Perfect story should score 100")
    void evaluate_perfectStory_scores100() {
        GapReport report = detector.evaluate(perfectStory());

        assertThat(report.getQualityScore()).isEqualTo(100);
        assertThat(report.isFlagged()).isFalse();
        assertThat(report.getFindings()).isEmpty();
    }

    @Test
    @DisplayName("Score — Story with only warnings should score > 40 (not flagged)")
    void evaluate_warningsOnly_notFlagged() {
        JiraIssue issue = perfectStory();
        issue.setStoryPoints(null);    // -7
        issue.setTimeSpentMinutes(0);  // -7

        GapReport report = detector.evaluate(issue);

        assertThat(report.getQualityScore()).isEqualTo(86);
        assertThat(report.isFlagged()).isFalse();
    }

    /*@Test
    @DisplayName("Score — Completely ungrouped story should score 0 and be flagged")
    void evaluate_completelyBadStory_scores0AndFlagged() {
        JiraIssue issue = JiraIssue.builder()
                .key("PROJ-999").summary("Do something")
                .description("").acceptanceCriteria(null)
                .issueType(IssueType.STORY).status(IssueStatus.IN_PROGRESS)
                .storyPoints(null).timeSpentMinutes(0)
                .statusChangedAt(LocalDate.now().minusDays(10)) // aging
                .build();

        GapReport report = detector.evaluate(issue);

        assertThat(report.getQualityScore()).isEqualTo(0);
        assertThat(report.isFlagged()).isTrue();
        assertThat(report.getCriticalCount()).isGreaterThan(0);
    }*/
}
