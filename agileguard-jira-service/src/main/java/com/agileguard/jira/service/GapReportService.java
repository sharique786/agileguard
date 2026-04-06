package com.agileguard.jira.service;

import com.agileguard.jira.entity.GapFindingEntity;
import com.agileguard.jira.model.GapReport;
import com.agileguard.jira.model.JiraIssue;
import com.agileguard.jira.model.SprintHealthReport;
import com.agileguard.jira.repository.GapFindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates gap detection across all issues in a sprint
 * and persists findings to the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GapReportService {

    private final JiraClientService jiraClientService;
    private final StoryGapDetector gapDetector;
    private final GapFindingRepository gapFindingRepository;

    /**
     * Runs a full gap scan for a project's active sprint.
     * Persists all findings and returns a SprintHealthReport.
     *
     * @param projectKey  JIRA project key
     * @param tenantId    the requesting tenant
     * @return sprint health report
     */
    @Transactional
    public SprintHealthReport scanActiveSprintGaps(String projectKey, String tenantId) {
        log.info("Starting gap scan for project: {} (tenant: {})", projectKey, tenantId);

        List<JiraIssue> issues = jiraClientService.getActiveSprintIssues(projectKey);
        List<GapReport> reports = issues.stream()
                .map(gapDetector::evaluate)
                .collect(Collectors.toList());

        // Persist all findings
        reports.stream()
                .flatMap(r -> r.getFindings().stream())
                .forEach(finding -> gapFindingRepository.save(GapFindingEntity.builder()
                        .tenantId(tenantId)
                        .projectId(projectKey)
                        .jiraIssueKey(finding.getIssueKey())
                        .gapType(finding.getType())
                        .severity(finding.getSeverity())
                        .details(finding.getMessage())
                        .suggestedAction(finding.getSuggestedAction())
                        .build()));

        // Compute health metrics
        double avgScore = reports.stream()
                .mapToInt(GapReport::getQualityScore).average().orElse(100.0);
        long flaggedCount = reports.stream().filter(GapReport::isFlagged).count();
        long withGapsCount = reports.stream()
                .filter(r -> !r.getFindings().isEmpty()).count();
        List<String> agingKeys = reports.stream()
                .filter(GapReport::hasCriticalFindings).map(GapReport::getIssueKey).toList();

        log.info("Scan complete: {} issues, {} flagged, avg score: {}",
                issues.size(), flaggedCount, (int) avgScore);

        return SprintHealthReport.builder()
                .projectKey(projectKey)
                .tenantId(tenantId)
                .totalIssues(issues.size())
                .issuesWithGaps((int) withGapsCount)
                .flaggedIssues((int) flaggedCount)
                .averageQualityScore(avgScore)
                .overallHealthScore((int) avgScore)
                .issueReports(reports)
                .agingIssueKeys(agingKeys)
                .build();
    }

    /** Returns all open gap findings for a tenant. */
    @Transactional(readOnly = true)
    public List<GapFindingEntity> getOpenGaps(String tenantId) {
        return gapFindingRepository.findOpenGapsByTenantOrderBySeverity(tenantId);
    }
}
