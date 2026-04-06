package com.agileguard.github.service;

import com.agileguard.github.model.LeaderboardEntry;
import com.agileguard.github.model.WorkflowRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes the developer/QA leaderboard by aggregating GitHub and JIRA metrics.
 *
 * Scoring breakdown (composite score 0-100):
 *   - Story quality score:     30%
 *   - Effort logging rate:     20%
 *   - Sub-task compliance:     15%
 *   - PR review contributions: 15%
 *   - CI/CD pass rate:         10%
 *   - Commit frequency:        10%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final GitHubClientService githubClient;

    /**
     * Builds the leaderboard for a project.
     * In a full implementation this would join data from the JIRA and GitHub APIs.
     * For the POC, returns demo data showing the scoring model.
     *
     * @param projectId    the project ID
     * @param githubOrg    GitHub organisation name
     * @return ranked list of contributors
     */
    public List<LeaderboardEntry> getLeaderboard(String projectId, String githubOrg) {
        log.info("Computing leaderboard for project: {}", projectId);

        // For POC: return realistic mock leaderboard
        List<LeaderboardEntry> entries = buildMockLeaderboard();
        rankEntries(entries);
        return entries;
    }

    /** Assigns rank 1..N based on composite score descending. */
    private void rankEntries(List<LeaderboardEntry> entries) {
        entries.sort(Comparator.comparingInt(LeaderboardEntry::getCompositeScore).reversed());
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }
    }

    private List<LeaderboardEntry> buildMockLeaderboard() {
        return new ArrayList<>(List.of(
            LeaderboardEntry.builder()
                .fullName("Esha Basu").githubUsername("esha-dev")
                .role("DEVELOPER").featureTeam("Sigma Team")
                .compositeScore(91).commitCount(28).prCount(6).prReviewCount(12)
                .ciPassRate(97).storiesDelivered(7).avgStoryQuality(88.5)
                .badges(List.of("QUALITY_CHAMPION", "TOP_REVIEWER"))
                .build(),
            LeaderboardEntry.builder()
                .fullName("Sachin Tester").githubUsername("sachin-qa")
                .role("QA_TESTER").featureTeam("Sigma Team")
                .compositeScore(84).commitCount(8).prCount(2).prReviewCount(5)
                .ciPassRate(90).storiesDelivered(9).avgStoryQuality(82.0)
                .badges(List.of("QA_CHAMPION"))
                .build(),
            LeaderboardEntry.builder()
                .fullName("Lokesh Dev").githubUsername("lokesh-dev")
                .role("DEVELOPER").featureTeam("Phoenix Team")
                .compositeScore(73).commitCount(15).prCount(4).prReviewCount(3)
                .ciPassRate(85).storiesDelivered(5).avgStoryQuality(70.0)
                .badges(List.of())
                .build()
        ));
    }
}
