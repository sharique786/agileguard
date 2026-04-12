package com.agileguard.jira.service;

import com.agileguard.common.enums.GapType;
import com.agileguard.jira.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all Epic-level JIRA operations:
 *
 *   createEpic(req)       — POST to JIRA (mock: returns synthesised JiraIssue)
 *   inspectEpic(epicKey)  — fetch linked stories + gap-detect each + aggregate metrics
 *   getStoriesForEpic(key)— fetch stories without gap analysis (AI context feed)
 *
 * In mock mode all three methods return realistic data that varies by epic key
 * so the Story Editor demo is meaningful without a real JIRA connection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EpicService {

    private final JiraClientService jiraClient;
    private final JiraLookupService lookupService;
    private final StoryGapDetector  gapDetector;

    @Value("${agileguard.jira.use-mock:true}")
    private boolean useMock;

    // ── Create Epic ───────────────────────────────────────────────────────────

    /**
     * Creates a new JIRA Epic issue and returns it.
     *
     * Real mode:  POST /rest/api/3/issue
     *             { fields: { issuetype: {name:"Epic"}, summary: epicName, ... } }
     * Mock mode:  synthesises a JiraIssue with a generated key.
     */
    public JiraIssue createEpic(CreateEpicRequest req) {
        log.info("Creating epic '{}' in project {}", req.getEpicName(), req.getProjectKey());
        // Real-mode implementation would call the JIRA REST API here.
        // For POC/mock mode we return a fully-populated synthetic issue.
        return buildMockEpic(req);
    }

    // ── Inspect Epic ──────────────────────────────────────────────────────────

    /**
     * Fetches all stories linked to the epic, runs gap detection on every story,
     * and returns an aggregated quality picture for the Story Editor.
     *
     * Real JQL:  "Epic Link" = {epicKey} OR parent = {epicKey}
     * Mock:      returns a curated set of stories with varied quality levels.
     *
     * @param epicKey  JIRA issue key of the epic (e.g. "PLAT-10")
     * @return         EpicInspectionResult with field names matching the Angular template
     */
    public EpicInspectionResult inspectEpic(String epicKey) {
        log.info("Inspecting epic: {}", epicKey);

        // 1. Resolve basic epic info (name, status)
        Optional<JiraEpic> epicOpt = lookupService.getEpic(epicKey);
        String epicName   = epicOpt.map(JiraEpic::getName).orElse(epicKey);
        String epicStatus = epicOpt.map(JiraEpic::getStatus).orElse("Unknown");

        // 2. Fetch all linked stories
        List<JiraIssue> stories = getStoriesForEpic(epicKey);
        log.info("Found {} stories linked to epic {}", stories.size(), epicKey);

        // 3. Run gap detection on every story
        List<GapReport> gapReports = stories.stream()
                .map(gapDetector::evaluate)
                .collect(Collectors.toList());

        // 4. Derive aggregated metrics
        int  totalStories = stories.size();
        long flaggedCount = gapReports.stream().filter(GapReport::isFlagged).count();
        int  totalGaps    = gapReports.stream()
                .mapToInt(g -> g.getFindings() != null ? g.getFindings().size() : 0)
                .sum();
        double avgScore = totalStories == 0 ? 100 :
                gapReports.stream().mapToInt(GapReport::getQualityScore).average().orElse(100);

        // 5. Count specific gap types using the enum directly (no string comparison)
        int missingAc = (int) gapReports.stream()
                .filter(g -> hasGapType(g, GapType.MISSING_ACCEPTANCE_CRITERIA))
                .count();
        int missingPts = (int) gapReports.stream()
                .filter(g -> hasGapType(g, GapType.MISSING_STORY_POINTS))
                .count();
        int aging = (int) stories.stream()
                .filter(s -> s.getDaysInCurrentStatus() > 5)
                .count();

        // 6. Build human-readable top-gap list for the summary panel
        List<String> topGaps = buildTopGaps(missingAc, missingPts, aging,
                (int) flaggedCount, avgScore, totalStories);

        // 7. Build narrative summary
        String summary = buildGapSummary(totalStories, totalGaps, (int) flaggedCount, avgScore);

        return EpicInspectionResult.builder()
                .epicKey(epicKey)
                .epicName(epicName)
                .epicStatus(epicStatus)
                // ── field names must match Angular template ──
                .storyCount(totalStories)
                .averageQualityScore(Math.round(avgScore * 10.0) / 10.0)
                .flaggedStoryCount((int) flaggedCount)
                .gapCount(totalGaps)
                .missingAcCount(missingAc)
                .missingPointsCount(missingPts)
                .agingCount(aging)
                .gapReports(gapReports)
                .topGaps(topGaps)
                .linkedStories(stories)
                .gapSummary(summary)
                .build();
    }

    // ── Get Stories ───────────────────────────────────────────────────────────

    /**
     * Returns all JIRA stories linked to an epic, without gap analysis.
     * Used by the AI service to feed epic context into its prompts.
     *
     * Real mode: JQL search filtered by epic link.
     * Mock mode: returns a fixed set of varied-quality stories.
     */
    public List<JiraIssue> getStoriesForEpic(String epicKey) {
        if (useMock) {
            return getMockStoriesForEpic(epicKey);
        }
        // Real: search JIRA for stories whose epicLink field matches this key
        String projectKey = epicKey.contains("-") ? epicKey.split("-")[0] : "PLAT";
        return jiraClient.getActiveSprintIssues(projectKey).stream()
                .filter(issue -> epicKey.equals(issue.getEpicLink()))
                .collect(Collectors.toList());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Tests whether a GapReport contains a finding of a given GapType.
     * Uses the enum directly — no brittle string comparison.
     */
    private boolean hasGapType(GapReport report, GapType type) {
        if (report.getFindings() == null) return false;
        return report.getFindings().stream()
                .anyMatch(f -> f.getType() == type);
    }

    private List<String> buildTopGaps(int missingAc, int missingPts, int aging,
                                       int flagged, double avgScore, int total) {
        List<String> gaps = new ArrayList<>();
        if (total == 0) return gaps;
        if (missingAc > 0)
            gaps.add(missingAc + " " + (missingAc == 1 ? "story is" : "stories are")
                    + " missing Acceptance Criteria");
        if (missingPts > 0)
            gaps.add(missingPts + " " + (missingPts == 1 ? "story has" : "stories have")
                    + " no story point estimate");
        if (aging > 0)
            gaps.add(aging + " " + (aging == 1 ? "story is" : "stories are")
                    + " aging (> 5 days in same status)");
        if (flagged > 0)
            gaps.add(flagged + " " + (flagged == 1 ? "story is" : "stories are")
                    + " flagged — quality score below 40 or CRITICAL finding");
        if (avgScore < 60 && gaps.isEmpty())
            gaps.add("Average quality score is low (" + Math.round(avgScore)
                    + "/100) — review all stories before sprint planning");
        return gaps;
    }

    private String buildGapSummary(int total, int totalGaps, int flagged, double avgScore) {
        if (total == 0) return "No stories are linked to this epic yet. "
                + "Use the Team Configuration section to generate stories with AI.";
        return String.format(
            "%d stories linked · %d gap findings across all stories · %d flagged. "
            + "Average quality score: %.0f/100. %s",
            total, totalGaps, flagged, avgScore,
            avgScore >= 75 ? "Epic is in good shape for sprint planning." :
            avgScore >= 50 ? "Several stories need attention before sprint start." :
                             "Significant quality issues — review required before sprint planning."
        );
    }

    // ── Mock data ─────────────────────────────────────────────────────────────

    private JiraIssue buildMockEpic(CreateEpicRequest req) {
        int num = 10 + (int)(Math.random() * 90);
        String key = req.getProjectKey() + "-" + num;
        return JiraIssue.builder()
                .id("epic-" + num)
                .key(key)
                .summary(req.getEpicName())
                .description(req.getDescription())
                .epicName(req.getEpicName())
                .priority(req.getPriority() != null ? req.getPriority() : "Medium")
                .businessLine(req.getBusinessLine())
                .projectKey(req.getProjectKey())
                .reporterAccountId(req.getReporterAccountId())
                .reporterName(req.getReporterName())
                .statusChangedAt(LocalDate.now())
                .createdAt(LocalDate.now())
                .build();
    }

    /**
     * Returns a realistic mix of well-groomed and poorly-groomed stories
     * so the inspection panel demonstrates the gap analysis meaningfully.
     */
    private List<JiraIssue> getMockStoriesForEpic(String epicKey) {
        String prefix = epicKey.contains("-") ? epicKey.split("-")[0] : "PLAT";
        return List.of(

            // Story 1 — fully groomed, good quality
            JiraIssue.builder()
                .id("s1").key(prefix + "-201")
                .summary("User authentication with SSO")
                .description("As a user, I want to log in with Google SSO "
                    + "so that I do not need a separate password for this platform.")
                .acceptanceCriteria("Given a user with a Google account, "
                    + "When they click 'Login with Google', "
                    + "Then they are authenticated and redirected to the dashboard within 3 seconds.\n\n"
                    + "Given invalid credentials, When SSO fails, "
                    + "Then a clear error message is displayed.")
                .epicLink(epicKey).epicName("Auth Platform Overhaul")
                .storyPoints(5).priority("High").businessLine("Platform")
                .statusChangedAt(LocalDate.now().minusDays(2))
                .build(),

            // Story 2 — missing description, missing AC, no points — multiple gaps
            JiraIssue.builder()
                .id("s2").key(prefix + "-202")
                .summary("Password reset flow")
                .description("")          // ERROR: missing description
                .acceptanceCriteria(null) // ERROR: missing AC
                .epicLink(epicKey).epicName("Auth Platform Overhaul")
                .storyPoints(null)        // WARNING: no estimate
                .priority("Medium").businessLine("Platform")
                .statusChangedAt(LocalDate.now().minusDays(1))
                .build(),

            // Story 3 — aging in same status, missing sub-tasks
            JiraIssue.builder()
                .id("s3").key(prefix + "-203")
                .summary("Session management and JWT token refresh")
                .description("As a user, I want my session to persist securely "
                    + "so that I do not need to re-authenticate during normal working hours.")
                .acceptanceCriteria("Session refresh is handled automatically — needs detail.")
                .epicLink(epicKey).epicName("Auth Platform Overhaul")
                .storyPoints(3).priority("Medium").businessLine("Platform")
                .statusChangedAt(LocalDate.now().minusDays(8)) // CRITICAL: aging > 5 days
                .build(),

            // Story 4 — acceptable but AC format is weak
            JiraIssue.builder()
                .id("s4").key(prefix + "-204")
                .summary("Multi-factor authentication via TOTP")
                .description("As a security-conscious user, I want to enable TOTP-based MFA "
                    + "on my account so that my account is protected even if my password is compromised.")
                .acceptanceCriteria("MFA should work correctly with authenticator apps.")
                .epicLink(epicKey).epicName("Auth Platform Overhaul")
                .storyPoints(8).priority("High").businessLine("Platform")
                .statusChangedAt(LocalDate.now().minusDays(3))
                .build()
        );
    }
}
