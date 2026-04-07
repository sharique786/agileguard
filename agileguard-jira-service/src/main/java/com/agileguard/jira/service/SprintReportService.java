package com.agileguard.jira.service;

import com.agileguard.common.enums.IssueStatus;
import com.agileguard.common.enums.IssueType;
import com.agileguard.jira.model.JiraIssue;
import com.agileguard.jira.model.JiraSprint;
import com.agileguard.jira.model.PiSprintReport;
import com.agileguard.jira.model.SprintStoryDetail;
import com.agileguard.jira.model.TeamSprintReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates SAFe-compliant sprint reports per feature team and at programme level.
 *
 * SAFe metric definitions used here:
 *   Predictability       = (Accepted Points / Committed Points) × 100
 *   Velocity             = Accepted story points (same as completed in SAFe)
 *   Story Points Committed = SP planned at sprint start
 *   Story Points Accepted  = SP of DONE stories at sprint close
 *   Stories Planned        = count of stories in sprint at planning
 *   Stories Accepted       = count of DONE stories
 *   Capacity Planned       = team_size × hours_per_person_per_sprint
 *   Capacity Actual        = Σ(time_spent_minutes) / 60 across all sprint stories
 *
 * In mock mode this service generates rich realistic data per feature team.
 * In real mode it delegates to JiraClientService and JiraLookupService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SprintReportService {

    private final JiraClientService  jiraClient;
    private final JiraLookupService  lookupService;

    @Value("${agileguard.jira.use-mock:true}")
    private boolean useMock;

    /** Standard hours per person per sprint (2-week sprint, 6 hrs/day productive). */
    private static final double HOURS_PER_PERSON_PER_SPRINT = 60.0;

    /** SAFe predictability target. */
    private static final double PREDICTABILITY_TARGET = 80.0;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates the full programme-level sprint report for all feature teams.
     *
     * @param projectKey  JIRA project key (e.g. "COMMSSURV")
     * @param sprintId    numeric sprint ID (null = active sprint)
     * @param piName      PI label (e.g. "PI-7") — optional, display-only
     * @return PiSprintReport aggregating all TeamSprintReports
     */
    public PiSprintReport generateSprintReport(String projectKey, Long sprintId, String piName) {
        log.info("Generating sprint report for project={} sprint={} pi={}",
                 projectKey, sprintId, piName);

        if (useMock) {
            return buildMockPiReport(projectKey, sprintId, piName);
        }
        return buildRealPiReport(projectKey, sprintId, piName);
    }

    /**
     * Returns all sprints (closed + active + future) for a project so the UI
     * can populate the sprint selector dropdown.
     */
    public List<JiraSprint> getSprintsForProject(String projectKey) {
        return lookupService.searchSprints(projectKey, "");
    }

    // ── Real-data path ─────────────────────────────────────────────────────────

    private PiSprintReport buildRealPiReport(String projectKey, Long sprintId, String piName) {
        // Fetch issues for the sprint; group by feature team (component)
        List<JiraIssue> allIssues = jiraClient.getActiveSprintIssues(projectKey);
        List<JiraSprint> sprints  = lookupService.searchSprints(projectKey, "");

        JiraSprint sprint = sprints.stream()
                .filter(s -> sprintId == null ? "active".equals(s.getState())
                                              : sprintId.equals(s.getId()))
                .findFirst()
                .orElse(sprints.isEmpty() ? null : sprints.get(0));

        // Group issues by first teamName (or component) to approximate feature team
        Map<String, List<JiraIssue>> byTeam = allIssues.stream()
                .collect(Collectors.groupingBy(i ->
                        i.getTeamNames() != null && !i.getTeamNames().isEmpty()
                            ? i.getTeamNames().get(0)
                            : (!i.getComponents().isEmpty() ? i.getComponents().get(0) : "Unassigned")));

        List<TeamSprintReport> teamReports = byTeam.entrySet().stream()
                .map(e -> buildTeamReport(e.getKey(), null, projectKey, e.getValue(), sprint, piName))
                .collect(Collectors.toList());

        return aggregateToPiReport(projectKey, piName, sprint, teamReports);
    }

    // ── Mock-data path ─────────────────────────────────────────────────────────

    private PiSprintReport buildMockPiReport(String projectKey, Long sprintId, String piName) {
        String pi = (piName != null && !piName.isBlank()) ? piName : "PI-7";

        // Simulate sprints list (matching JiraLookupService mock)
        JiraSprint sprint = resolveMockSprint(projectKey, sprintId);

        List<TeamSprintReport> teamReports = List.of(
            buildMockTeamReport("ft-001", "Sigma Team",    projectKey, "Sigma",       sprint, pi),
            buildMockTeamReport("ft-002", "Phoenix Team",        projectKey, "Phoenix", sprint, pi),
            buildMockTeamReport("ft-003", "Incredible Team",projectKey,"Incredible",  sprint, pi),
            buildMockTeamReport("ft-004", "Invincible Team",      projectKey, "Invincible",         sprint, pi)
        );

        return aggregateToPiReport(projectKey, pi, sprint, teamReports);
    }

    private JiraSprint resolveMockSprint(String projectKey, Long sprintId) {
        String prefix = projectKey != null ? projectKey : "COMMSSURV";
        if (sprintId == null || sprintId == 42L) {
            return JiraSprint.builder()
                    .id(42L).name(prefix + " Sprint 42 — Platform Hardening")
                    .state("active").goal("Stabilise payments, close aging stories")
                    .startDate(LocalDate.now().minusDays(7))
                    .endDate(LocalDate.now().plusDays(7)).build();
        }
        if (sprintId == 41L) {
            return JiraSprint.builder()
                    .id(41L).name(prefix + " Sprint 41 — Q1 Wrap-up")
                    .state("closed").goal("Complete Q1 regulatory stories")
                    .startDate(LocalDate.now().minusDays(21))
                    .endDate(LocalDate.now().minusDays(7)).build();
        }
        return JiraSprint.builder()
                .id(43L).name(prefix + " Sprint 43 — Auth & Observability")
                .state("future").build();
    }

    /**
     * Builds a realistic mock TeamSprintReport for a named feature team.
     * Generates varied data so each team tells a different story.
     */
    private TeamSprintReport buildMockTeamReport(
            String teamId, String teamName, String projectKey,
            String component, JiraSprint sprint, String piName) {

        // Each team has different characteristics to demonstrate the dashboard
        return switch (teamName) {
            case "Sigma Team"       -> paymentsTeamReport(teamId, teamName, projectKey, component, sprint, piName);
            case "Phoenix Team"           -> authTeamReport(teamId, teamName, projectKey, component, sprint, piName);
            case "Incredible Team"  -> notificationsTeamReport(teamId, teamName, projectKey, component, sprint, piName);
            default                    -> searchTeamReport(teamId, teamName, projectKey, component, sprint, piName);
        };
    }

    // ─── Per-team mock builders ──────────────────────────────────────────────

    private TeamSprintReport paymentsTeamReport(
            String id, String name, String pk, String comp, JiraSprint sprint, String pi) {
        // Strong sprint — high predictability
        List<SprintStoryDetail> completed = List.of(
            story(pk+"-101","Tokenise card data at rest",             8, "DONE","Priya Sharma","PAY-5","Payment Gateway Overhaul"),
            story(pk+"-102","3DS2 redirect flow implementation",      5, "DONE","Priya Sharma","PAY-5","Payment Gateway Overhaul"),
            story(pk+"-103","Retry failed transactions — idempotency",3, "DONE","Amit Dev",    "PAY-5","Payment Gateway Overhaul"),
            story(pk+"-104","Webhook delivery confirmation",           3, "DONE","Amit Dev",    "PAY-5","Payment Gateway Overhaul"),
            story(pk+"-105","Payment reconciliation daily job",        5, "DONE","Ravi Tester", "PAY-6","Reporting Pipeline")
        );
        List<SprintStoryDetail> spilled = List.of(
            spilledStory(pk+"-106","Multi-currency support — EUR/GBP",8,"IN_PROGRESS",
                         "External dependency — FX rate API not available","Sprint 43")
        );
        return buildReport(id,name,pk,comp,sprint,pi, completed, spilled, List.of(),
                           4, 90.0, "UP");
    }

    private TeamSprintReport authTeamReport(
            String id, String name, String pk, String comp, JiraSprint sprint, String pi) {
        // Below-target sprint — 73% predictability
        List<SprintStoryDetail> completed = List.of(
            story(pk+"-201","Google SSO integration",                 5,"DONE","Kiran Lead",  "AUTH-2","Auth Platform Overhaul"),
            story(pk+"-202","JWT refresh token rotation",             3,"DONE","Kiran Lead",  "AUTH-2","Auth Platform Overhaul"),
            story(pk+"-203","Password strength policy enforcement",   2,"DONE","Neha Product","AUTH-2","Auth Platform Overhaul")
        );
        List<SprintStoryDetail> spilled = List.of(
            spilledStory(pk+"-204","MFA — TOTP app support",          5,"IN_PROGRESS",
                         "Scope larger than estimated — split into sub-tasks","Sprint 43"),
            spilledStory(pk+"-205","Session revocation on logout",     3,"BLOCKED",
                         "Blocked on Auth DB migration completion","Sprint 43")
        );
        return buildReport(id,name,pk,comp,sprint,pi, completed, spilled, List.of(),
                           3, 75.0, "DOWN");
    }

    private TeamSprintReport notificationsTeamReport(
            String id, String name, String pk, String comp, JiraSprint sprint, String pi) {
        // Perfect sprint — 100% predictability
        List<SprintStoryDetail> completed = List.of(
            story(pk+"-301","Order status email notifications",        5,"DONE","Dev User",   "NOTIF-1","Notification Engine"),
            story(pk+"-302","SMS alert for failed payment",            3,"DONE","Dev User",   "NOTIF-1","Notification Engine"),
            story(pk+"-303","Notification preference centre",          5,"DONE","QA User",    "NOTIF-1","Notification Engine"),
            story(pk+"-304","Email template redesign — brand refresh", 2,"DONE","Dev User",   "NOTIF-2","Brand Refresh")
        );
        List<SprintStoryDetail> spilled = List.of();
        // One story added mid-sprint (scope creep)
        List<SprintStoryDetail> added = List.of(
            addedStory(pk+"-305","Push notification — order dispatched", 3,"DONE","Dev User","NOTIF-1","Notification Engine")
        );
        return buildReport(id,name,pk,comp,sprint,pi, completed, spilled, added,
                           2, 52.0, "STABLE");
    }

    private TeamSprintReport searchTeamReport(
            String id, String name, String pk, String comp, JiraSprint sprint, String pi) {
        // Mid-range sprint
        List<SprintStoryDetail> completed = List.of(
            story(pk+"-401","Elasticsearch product index rebuild",     8,"DONE","Dev User",   "SRCH-3","Search V2"),
            story(pk+"-402","Faceted search — category filter",        5,"DONE","Dev User",   "SRCH-3","Search V2"),
            story(pk+"-403","Typo-tolerance — fuzzy matching",         3,"DONE","Priya Sharma","SRCH-3","Search V2")
        );
        List<SprintStoryDetail> spilled = List.of(
            spilledStory(pk+"-404","Autocomplete suggestions API",     5,"READY_FOR_QA",
                         "QA raised defects — fix in progress","Sprint 43")
        );
        return buildReport(id,name,pk,comp,sprint,pi, completed, spilled, List.of(),
                           3, 72.0, "UP");
    }

    // ─── Generic report builder ──────────────────────────────────────────────

    private TeamSprintReport buildReport(
            String id, String name, String pk, String comp,
            JiraSprint sprint, String pi,
            List<SprintStoryDetail> completed,
            List<SprintStoryDetail> spilled,
            List<SprintStoryDetail> added,
            int teamSize,
            double capacityActualHours,
            String trend) {

        int committed = sumPoints(completed) + sumPoints(spilled);
        int accepted  = sumPoints(completed);
        int spilledPts= sumPoints(spilled);
        double pred   = committed > 0 ? Math.round((accepted * 100.0 / committed) * 10) / 10.0 : 0;
        double capPlanned = teamSize * HOURS_PER_PERSON_PER_SPRINT;

        return TeamSprintReport.builder()
                .featureTeamId(id).featureTeamName(name)
                .projectKey(pk).jiraComponent(comp)
                .piName(pi)
                .sprintId(sprint.getId()).sprintName(sprint.getName())
                .sprintGoal(sprint.getGoal()).sprintState(sprint.getState())
                .sprintStartDate(sprint.getStartDate()).sprintEndDate(sprint.getEndDate())
                .storyPointsCommitted(committed)
                .storyPointsCompleted(accepted)
                .storyPointsAccepted(accepted)
                .storyPointsSpilled(spilledPts)
                .predictability(pred)
                .velocity(accepted)
                .storiesPlanned(completed.size() + spilled.size())
                .storiesAccepted(completed.size())
                .storiesSpilled(spilled.size())
                .storiesAdded(added.size())
                .storiesRemoved(0)
                .capacityPlanned(capPlanned)
                .capacityActual(capacityActualHours)
                .capacityUtilisation(capPlanned > 0 ?
                        Math.round((capacityActualHours / capPlanned) * 1000) / 10.0 : null)
                .teamSize(teamSize)
                .completedStories(new ArrayList<>(completed))
                .spilledStories(new ArrayList<>(spilled))
                .addedStories(new ArrayList<>(added))
                .meetsPredictabilityTarget(pred >= PREDICTABILITY_TARGET)
                .velocityTrend(trend)
                .summary(buildSummary(name, pred, completed.size(), spilled.size(), trend))
                .generatedAt(Instant.now())
                .build();
    }

    private TeamSprintReport buildTeamReport(
            String teamName, String teamId, String projectKey,
            List<JiraIssue> issues, JiraSprint sprint, String piName) {

        List<SprintStoryDetail> completed = issues.stream()
                .filter(i -> i.getStatus() == IssueStatus.DONE)
                .map(i -> toDetail(i, "COMPLETED", null, null))
                .collect(Collectors.toList());

        List<SprintStoryDetail> spilled = issues.stream()
                .filter(i -> i.getStatus() != IssueStatus.DONE)
                .map(i -> toDetail(i, "SPILLED", null, null))
                .collect(Collectors.toList());

        return buildReport(teamId != null ? teamId : UUID.randomUUID().toString(),
                teamName, projectKey, teamName, sprint, piName,
                completed, spilled, List.of(), 3, 0, "UNKNOWN");
    }

    // ─── Story detail factory helpers ────────────────────────────────────────

    private SprintStoryDetail story(String key, String summary, int pts,
            String status, String assignee, String epicKey, String epicName) {
        return SprintStoryDetail.builder()
                .issueKey(key).summary(summary).storyPoints(pts)
                .status(IssueStatus.DONE).issueType(IssueType.STORY)
                .assigneeName(assignee).epicLink(epicKey).epicName(epicName)
                .reportCategory("COMPLETED").hasDevSubTask(true).hasQaSubTask(true)
                .timeSpentMinutes(pts * 90)
                .build();
    }

    private SprintStoryDetail spilledStory(String key, String summary, int pts,
            String status, String reason, String movedTo) {
        IssueStatus st = switch (status) {
            case "IN_PROGRESS"  -> IssueStatus.IN_PROGRESS;
            case "BLOCKED"      -> IssueStatus.BLOCKED;
            case "READY_FOR_QA" -> IssueStatus.READY_FOR_QA;
            default -> IssueStatus.IN_PROGRESS;
        };
        return SprintStoryDetail.builder()
                .issueKey(key).summary(summary).storyPoints(pts)
                .status(st).issueType(IssueType.STORY)
                .reportCategory("SPILLED")
                .spillReason(reason).movedToSprint(movedTo)
                .hasDevSubTask(true).hasQaSubTask(false)
                .timeSpentMinutes(pts * 30)
                .build();
    }

    private SprintStoryDetail addedStory(String key, String summary, int pts,
            String status, String assignee, String epicKey, String epicName) {
        return SprintStoryDetail.builder()
                .issueKey(key).summary(summary).storyPoints(pts)
                .status(IssueStatus.DONE).issueType(IssueType.STORY)
                .assigneeName(assignee).epicLink(epicKey).epicName(epicName)
                .reportCategory("ADDED").hasDevSubTask(true).hasQaSubTask(true)
                .timeSpentMinutes(pts * 85)
                .build();
    }

    private SprintStoryDetail toDetail(JiraIssue i, String category,
            String reason, String movedTo) {
        return SprintStoryDetail.builder()
                .issueKey(i.getKey()).summary(i.getSummary())
                .storyPoints(i.getStoryPoints()).status(i.getStatus())
                .issueType(i.getIssueType()).assigneeName(i.getAssigneeName())
                .epicLink(i.getEpicLink()).epicName(i.getEpicName())
                .reportCategory(category).spillReason(reason).movedToSprint(movedTo)
                .hasDevSubTask(i.hasDevSubTask()).hasQaSubTask(i.hasQaSubTask())
                .timeSpentMinutes(i.getTimeSpentMinutes())
                .build();
    }

    // ─── Aggregation ──────────────────────────────────────────────────────────

    private PiSprintReport aggregateToPiReport(String projectKey, String piName,
            JiraSprint sprint, List<TeamSprintReport> teamReports) {

        int committed = teamReports.stream().mapToInt(TeamSprintReport::getStoryPointsCommitted).sum();
        int accepted  = teamReports.stream().mapToInt(TeamSprintReport::getStoryPointsAccepted).sum();

        return PiSprintReport.builder()
                .projectKey(projectKey).piName(piName)
                .sprintId(sprint != null ? sprint.getId() : null)
                .sprintName(sprint != null ? sprint.getName() : "Unknown Sprint")
                .sprintState(sprint != null ? sprint.getState() : "unknown")
                .sprintGoal(sprint != null ? sprint.getGoal() : null)
                .sprintStartDate(sprint != null && sprint.getStartDate() != null
                        ? sprint.getStartDate().toString() : null)
                .sprintEndDate(sprint != null && sprint.getEndDate() != null
                        ? sprint.getEndDate().toString() : null)
                .totalPointsCommitted(committed)
                .totalPointsAccepted(accepted)
                .programmePredictability(committed > 0
                        ? Math.round((accepted * 100.0 / committed) * 10) / 10.0 : null)
                .totalVelocity(teamReports.stream().mapToInt(TeamSprintReport::getVelocity).sum())
                .totalStoriesPlanned(teamReports.stream().mapToInt(TeamSprintReport::getStoriesPlanned).sum())
                .totalStoriesAccepted(teamReports.stream().mapToInt(TeamSprintReport::getStoriesAccepted).sum())
                .totalStoriesSpilled(teamReports.stream().mapToInt(TeamSprintReport::getStoriesSpilled).sum())
                .totalCapacityPlanned(teamReports.stream().mapToDouble(TeamSprintReport::getCapacityPlanned).sum())
                .totalCapacityActual(teamReports.stream().mapToDouble(TeamSprintReport::getCapacityActual).sum())
                .teamReports(teamReports)
                .generatedAt(Instant.now())
                .build();
    }

    private int sumPoints(List<SprintStoryDetail> stories) {
        return stories.stream()
                .mapToInt(s -> s.getStoryPoints() != null ? s.getStoryPoints() : 0).sum();
    }

    private String buildSummary(String team, double pred, int completed, int spilled, String trend) {
        String predLabel = pred >= 100 ? "Perfect sprint" : pred >= 80 ? "Strong sprint" : "Challenging sprint";
        String trendStr  = "UP".equals(trend) ? " Velocity trending up." : "DOWN".equals(trend) ? " Velocity down vs last sprint." : "";
        return String.format("%s for %s — %.0f%% predictability. %d stories accepted, %d spilled.%s",
                predLabel, team, pred, completed, spilled, trendStr);
    }
}
