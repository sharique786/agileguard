package com.agileguard.github.model;

import lombok.*;

/** A single entry in the developer/QA leaderboard. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaderboardEntry {
    private int rank;
    private String userId;
    private String fullName;
    private String githubUsername;
    private String role;
    private String featureTeam;
    private int compositeScore;    // 0-100

    // Score breakdown
    private int commitCount;
    private int prCount;
    private int prReviewCount;
    private int ciPassRate;        // percentage
    private int storiesDelivered;
    private double avgStoryQuality;

    private java.util.List<String> badges;
}
