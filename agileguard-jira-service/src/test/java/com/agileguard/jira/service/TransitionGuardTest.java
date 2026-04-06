package com.agileguard.jira.service;

import com.agileguard.common.enums.IssueStatus;
import com.agileguard.common.enums.IssueType;
import com.agileguard.common.exception.AgileGuardException;
import com.agileguard.jira.model.JiraIssue;
import com.agileguard.jira.model.JiraSubTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TransitionGuard.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransitionGuard Tests")
class TransitionGuardTest {

    @InjectMocks
    private TransitionGuard guard;

    private JiraIssue fullReadyIssue() {
        return JiraIssue.builder()
                .key("PROJ-1").issueType(IssueType.STORY)
                .status(IssueStatus.IN_PROGRESS)
                .description("Full description here").acceptanceCriteria("Given...When...Then")
                .storyPoints(3).timeSpentMinutes(120)
                .subTasks(List.of(
                    JiraSubTask.builder().type("Dev Task").status("Done").build(),
                    JiraSubTask.builder().type("QA Task").status("In Progress").build()))
                .linkedPullRequests(List.of("PR-1"))
                .build();
    }

    @Test
    @DisplayName("Should ALLOW transition to Ready for QA when all gates pass")
    void validateTransition_toReadyForQA_allGatesPass_allowed() {
        assertThatNoException()
                .isThrownBy(() -> guard.validateTransition(fullReadyIssue(), IssueStatus.READY_FOR_QA));
    }

    @Test
    @DisplayName("Should BLOCK transition to Ready for QA when no Dev sub-task")
    void validateTransition_toReadyForQA_noDevSubTask_blocked() {
        JiraIssue issue = fullReadyIssue();
        issue.setSubTasks(List.of()); // no sub-tasks

        assertThatThrownBy(() -> guard.validateTransition(issue, IssueStatus.READY_FOR_QA))
                .isInstanceOf(AgileGuardException.class)
                .hasMessageContaining("BLOCKED")
                .hasMessageContaining("developer sub-task");
    }

    @Test
    @DisplayName("Should BLOCK transition to Ready for QA when no effort logged")
    void validateTransition_toReadyForQA_noEffort_blocked() {
        JiraIssue issue = fullReadyIssue();
        issue.setTimeSpentMinutes(0);

        assertThatThrownBy(() -> guard.validateTransition(issue, IssueStatus.READY_FOR_QA))
                .isInstanceOf(AgileGuardException.class)
                .hasMessageContaining("effort");
    }

    @Test
    @DisplayName("Should BLOCK transition to Ready for QA when no PR linked")
    void validateTransition_toReadyForQA_noPR_blocked() {
        JiraIssue issue = fullReadyIssue();
        issue.setLinkedPullRequests(List.of());

        assertThatThrownBy(() -> guard.validateTransition(issue, IssueStatus.READY_FOR_QA))
                .isInstanceOf(AgileGuardException.class)
                .hasMessageContaining("pull request");
    }

    @Test
    @DisplayName("Should BLOCK transition to Ready for Release when AC is missing")
    void validateTransition_toReadyForRelease_missingAC_blocked() {
        JiraIssue issue = fullReadyIssue();
        issue.setAcceptanceCriteria("");

        assertThatThrownBy(() -> guard.validateTransition(issue, IssueStatus.READY_FOR_RELEASE))
                .isInstanceOf(AgileGuardException.class)
                .hasMessageContaining("Acceptance Criteria");
    }

    @Test
    @DisplayName("Should ALLOW In Progress → In Review (no gate)")
    void validateTransition_toInReview_alwaysAllowed() {
        assertThatNoException()
                .isThrownBy(() -> guard.validateTransition(fullReadyIssue(), IssueStatus.IN_REVIEW));
    }
}
