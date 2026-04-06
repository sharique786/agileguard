package com.agileguard.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Summary returned after a successful tenant onboarding.
 * Contains IDs and names of all created entities so the
 * frontend can navigate directly to the new workspace.
 */
@Data
@Builder
public class OnboardingResponse {
    private String tenantId;
    private String tenantName;
    private String tenantSlug;
    private String projectTeamId;
    private String projectTeamName;
    private List<FeatureTeamSummary> featureTeams;
    private String adminUserId;
    private String adminEmail;
    private AuthResponse tokens;    // JWT tokens for the newly created admin

    @Data
    @Builder
    public static class FeatureTeamSummary {
        private String id;
        private String name;
    }
}
