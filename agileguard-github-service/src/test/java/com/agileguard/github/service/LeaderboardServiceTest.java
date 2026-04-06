package com.agileguard.github.service;

import com.agileguard.github.model.LeaderboardEntry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardService Tests")
class LeaderboardServiceTest {

    @InjectMocks private LeaderboardService leaderboardService;
    @Mock private GitHubClientService githubClient;

    @Test
    @DisplayName("getLeaderboard — Should return ranked entries sorted by score")
    void getLeaderboard_returnsSortedEntries() {
        List<LeaderboardEntry> entries = leaderboardService.getLeaderboard("proj-001", "db");

        assertThat(entries).isNotEmpty();
        assertThat(entries.get(0).getRank()).isEqualTo(1);
        // Verify rank 1 has highest score
        assertThat(entries.get(0).getCompositeScore())
                .isGreaterThanOrEqualTo(entries.get(1).getCompositeScore());
    }

    @Test
    @DisplayName("getLeaderboard — All entries should have rank assigned")
    void getLeaderboard_allEntriesHaveRank() {
        List<LeaderboardEntry> entries = leaderboardService.getLeaderboard("proj-001", "db");
        assertThat(entries).allMatch(e -> e.getRank() > 0);
    }

    @Test
    @DisplayName("getLeaderboard — Scores should be between 0 and 100")
    void getLeaderboard_scoresInValidRange() {
        List<LeaderboardEntry> entries = leaderboardService.getLeaderboard("proj-001", "db");
        assertThat(entries).allMatch(e -> e.getCompositeScore() >= 0 && e.getCompositeScore() <= 100);
    }
}
