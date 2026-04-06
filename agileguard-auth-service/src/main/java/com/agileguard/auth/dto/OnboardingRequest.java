package com.agileguard.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Single-shot onboarding request that creates a complete tenant hierarchy
 * in one API call:
 *   Tenant → Project Team → Feature Teams → Admin User
 *
 * All nested objects are validated independently via @Valid.
 */
@Data
public class OnboardingRequest {

    @Valid
    @NotNull(message = "Organisation details are required")
    private OrganisationDetails organisation;

    @Valid
    @NotNull(message = "Project team details are required")
    private ProjectTeamDetails projectTeam;

    @Valid
    private List<FeatureTeamDetails> featureTeams;

    @Valid
    @NotNull(message = "Admin user details are required")
    private AdminUserDetails adminUser;

    // ── Nested DTOs ──────────────────────────────────────────────────────────

    @Data
    public static class OrganisationDetails {
        @NotBlank(message = "Organisation name is required")
        @Size(min = 2, max = 100)
        private String name;

        @NotBlank
        @Pattern(regexp = "^[a-z0-9-]+$",
                 message = "Slug must be lowercase letters, numbers, and hyphens only")
        @Size(min = 2, max = 50)
        private String slug;

        private String plan = "FREE";

        /** Atlassian JIRA base URL — optional at onboarding, can be configured later. */
        private String jiraBaseUrl;
        private String jiraUserEmail;
        private String jiraApiToken;
    }

    @Data
    public static class ProjectTeamDetails {
        @NotBlank(message = "Project team name is required")
        @Size(min = 2, max = 100)
        private String name;

        private String jiraProjectKey;
        private String githubOrg;
        private String description;
    }

    @Data
    public static class FeatureTeamDetails {
        @NotBlank(message = "Feature team name is required")
        @Size(min = 2, max = 100)
        private String name;

        private String jiraComponent;
        private String githubRepos;
        private String description;
    }

    @Data
    public static class AdminUserDetails {
        @NotBlank @Email(message = "Valid admin email is required")
        private String email;

        @NotBlank
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        private String fullName;

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        private String githubUsername;
        private String jiraAccountId;
    }
}
