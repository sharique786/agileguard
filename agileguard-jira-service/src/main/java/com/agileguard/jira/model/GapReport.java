package com.agileguard.jira.model;

import com.agileguard.common.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated gap analysis report for a single JIRA issue.
 * Includes a quality score (0-100) and all detected findings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapReport {

    private String issueKey;
    private String issueSummary;
    private int qualityScore;           // 0-100; higher is better
    private boolean flagged;            // true when score < 40 or has CRITICAL finding
    private List<GapFinding> findings;
    private int criticalCount;
    private int errorCount;
    private int warningCount;

    @Builder.Default
    private Instant evaluatedAt = Instant.now();

    /** Returns true if any CRITICAL findings were detected. */
    public boolean hasCriticalFindings() {
        return findings.stream().anyMatch(f -> f.getSeverity() == Severity.CRITICAL);
    }

    /** Returns all findings of a given severity. */
    public List<GapFinding> findingsBySeverity(Severity severity) {
        return findings.stream().filter(f -> f.getSeverity() == severity).toList();
    }
}
