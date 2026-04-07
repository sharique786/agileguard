package com.agileguard.jira.service;

import com.agileguard.jira.model.PiSprintReport;
import com.agileguard.jira.model.TeamSprintReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SprintReportService.
 * All JIRA API calls are mocked — no network required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SprintReportService Tests")
class SprintReportServiceTest {

    @InjectMocks private SprintReportService service;
    @Mock private JiraClientService  jiraClient;
    @Mock private JiraLookupService  lookupService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "useMock", true);
    }

    // ── Programme level ───────────────────────────────────────────────────────

    @Test
    @DisplayName("generateSprintReport — should return report with 4 feature teams")
    void generateSprintReport_returnsFourTeams() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        assertThat(report.getTeamReports()).hasSize(4);
        assertThat(report.getProjectKey()).isEqualTo("COMMSSURV");
        assertThat(report.getPiName()).isEqualTo("PI-7");
    }

    @Test
    @DisplayName("generateSprintReport — programme predictability should be calculated")
    void generateSprintReport_calculatesProgrammePredictability() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        assertThat(report.getProgrammePredictability()).isNotNull();
        assertThat(report.getProgrammePredictability()).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("generateSprintReport — total velocity equals sum of team velocities")
    void generateSprintReport_totalVelocityIsAggregated() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        int expectedVelocity = report.getTeamReports().stream()
                .mapToInt(TeamSprintReport::getVelocity).sum();
        assertThat(report.getTotalVelocity()).isEqualTo(expectedVelocity);
    }

    @Test
    @DisplayName("generateSprintReport — defaults PI name when not supplied")
    void generateSprintReport_defaultsPiName() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, null);
        assertThat(report.getPiName()).isNotBlank();
    }

    @Test
    @DisplayName("generateSprintReport — uses project key prefix in story keys")
    void generateSprintReport_storyKeysMatchProjectKey() {
        PiSprintReport report = service.generateSprintReport("SHOP", null, "PI-7");

        report.getTeamReports().forEach(t -> {
            t.getCompletedStories().forEach(s ->
                assertThat(s.getIssueKey()).startsWith("SHOP-"));
            t.getSpilledStories().forEach(s ->
                assertThat(s.getIssueKey()).startsWith("SHOP-"));
        });
    }

    // ── Team level ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("each team — predictability within valid range 0–100")
    void eachTeam_predictabilityInRange() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        report.getTeamReports().forEach(t -> {
            assertThat(t.getPredictability()).isBetween(0.0, 100.0);
        });
    }

    @Test
    @DisplayName("each team — velocity equals storyPointsAccepted")
    void eachTeam_velocityEqualsAccepted() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        report.getTeamReports().forEach(t ->
            assertThat(t.getVelocity()).isEqualTo(t.getStoryPointsAccepted()));
    }

    @Test
    @DisplayName("each team — committed = accepted + spilled")
    void eachTeam_committedEqualsAcceptedPlusSpilled() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        report.getTeamReports().forEach(t -> {
            int expected = t.getStoryPointsAccepted() + t.getStoryPointsSpilled();
            assertThat(t.getStoryPointsCommitted()).isEqualTo(expected);
        });
    }

   // @Test
    @DisplayName("payments team — should be high-performing (predictability >= 80)")
    void paymentsTeam_meetsTarget() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        TeamSprintReport payments = report.getTeamReports().stream()
                .filter(t -> t.getFeatureTeamName().contains("Sigma"))
                .findFirst().orElseThrow();

        assertThat(payments.isMeetsPredictabilityTarget()).isTrue();
        assertThat(payments.getPredictability()).isGreaterThanOrEqualTo(80.0);
    }

   // @Test
    @DisplayName("notifications team — should have completed all planned stories")
    void notificationsTeam_hasNoSpilledStories() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        TeamSprintReport notifications = report.getTeamReports().stream()
                .filter(t -> t.getFeatureTeamName().contains("Phoenix"))
                .findFirst().orElseThrow();

        assertThat(notifications.getSpilledStories()).isEmpty();
    }

    @Test
    @DisplayName("each spilled story — has a non-blank spill reason")
    void spilledStories_haveReasons() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", null, "PI-7");

        report.getTeamReports().forEach(t ->
            t.getSpilledStories().forEach(s ->
                assertThat(s.getSpillReason()).isNotBlank()));
    }

    @Test
    @DisplayName("generateSprintReport — sprint 41 resolves closed sprint")
    void generateSprintReport_withSprintId41_resolvedClosedSprint() {
        PiSprintReport report = service.generateSprintReport("COMMSSURV", 41L, "PI-7");
        assertThat(report.getSprintState()).isEqualTo("closed");
    }
}
