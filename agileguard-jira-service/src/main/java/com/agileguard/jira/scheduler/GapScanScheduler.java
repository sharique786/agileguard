package com.agileguard.jira.scheduler;

import com.agileguard.jira.service.GapReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job that runs a gap scan for all configured projects.
 * Runs every 15 minutes by default (configurable via cron expression).
 * On startup, waits 30 seconds before the first scan to allow other services to start.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GapScanScheduler {

    private final GapReportService gapReportService;

    @Value("${agileguard.jira.scan-project-keys:COMMSSURV}")
    private String projectKeysConfig;

    @Value("${agileguard.jira.scan-tenant-id:demo}")
    private String scanTenantId;

    /**
     * Runs gap scans for all configured projects.
     * Triggered by cron (default: every 15 minutes).
     */
    @Scheduled(initialDelay = 30000, fixedDelayString = "${agileguard.jira.scan-interval-ms:900000}")
    public void runScheduledGapScan() {
        String[] projectKeys = projectKeysConfig.split(",");
        log.info("Scheduled gap scan starting for {} projects", projectKeys.length);

        for (String projectKey : projectKeys) {
            try {
                var report = gapReportService.scanActiveSprintGaps(
                        projectKey.trim(), scanTenantId);
                log.info("Scan complete for {}: {} issues, {} flagged, score={}",
                        projectKey.trim(), report.getTotalIssues(),
                        report.getFlaggedIssues(), report.getOverallHealthScore());
            } catch (Exception e) {
                log.error("Gap scan failed for project {}: {}", projectKey, e.getMessage());
            }
        }
    }
}
