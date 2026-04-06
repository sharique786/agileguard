package com.agileguard.auth.service;

import com.agileguard.auth.dto.FeatureTeamRequest;
import com.agileguard.auth.dto.JiraConnectionTestRequest;
import com.agileguard.auth.dto.JiraConnectionTestResponse;
import com.agileguard.auth.dto.ProjectTeamRequest;
import com.agileguard.auth.dto.UpdateTenantRequest;
import com.agileguard.auth.dto.UpdateUserRoleRequest;
import com.agileguard.auth.dto.UserResponse;
import com.agileguard.auth.entity.AppUser;
import com.agileguard.auth.entity.FeatureTeam;
import com.agileguard.auth.entity.ProjectTeam;
import com.agileguard.auth.entity.Tenant;
import com.agileguard.auth.repository.FeatureTeamRepository;
import com.agileguard.auth.repository.ProjectTeamRepository;
import com.agileguard.auth.repository.TenantRepository;
import com.agileguard.auth.repository.UserRepository;
import com.agileguard.common.exception.AgileGuardException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Extended tenant management operations:
 *   – Update / deactivate tenants, project teams, feature teams
 *   – User listing and role management within a tenant
 *   – JIRA connection validation (called before credentials are saved)
 *
 * Separated from the original TenantService to preserve its contract
 * while adding the richer management layer needed for the onboarding UI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantManagementService {

    private final TenantRepository      tenantRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final FeatureTeamRepository featureTeamRepository;
    private final UserRepository        userRepository;

    // ── Tenant CRUD ───────────────────────────────────────────────────────────

    /**
     * Partially updates a tenant's mutable fields.
     * Null fields in the request are ignored (PATCH semantics).
     */
    @Transactional
    public Tenant updateTenant(String tenantId, UpdateTenantRequest req) {
        Tenant tenant = requireTenant(tenantId);
        if (StringUtils.hasText(req.getName()))        tenant.setName(req.getName());
        if (StringUtils.hasText(req.getJiraBaseUrl())) tenant.setJiraBaseUrl(req.getJiraBaseUrl());
        if (StringUtils.hasText(req.getJiraUserEmail()))tenant.setJiraUserEmail(req.getJiraUserEmail());
        if (StringUtils.hasText(req.getJiraApiToken())) tenant.setJiraApiToken(req.getJiraApiToken());
        if (req.getActive() != null)                    tenant.setActive(req.getActive());
        if (StringUtils.hasText(req.getPlan())) {
            try { tenant.setPlan(Tenant.Plan.valueOf(req.getPlan().toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }
        log.info("Updated tenant '{}' ({})", tenant.getName(), tenantId);
        return tenantRepository.save(tenant);
    }

    /** Soft-deletes a tenant by setting active = false. */
    @Transactional
    public void deactivateTenant(String tenantId) {
        Tenant tenant = requireTenant(tenantId);
        tenant.setActive(false);
        tenantRepository.save(tenant);
        log.info("Deactivated tenant '{}' ({})", tenant.getName(), tenantId);
    }

    // ── Project Team CRUD ─────────────────────────────────────────────────────

    /** Updates a project team's name, JIRA key, or GitHub org. */
    @Transactional
    public ProjectTeam updateProjectTeam(String projectTeamId, ProjectTeamRequest req) {
        ProjectTeam team = projectTeamRepository.findById(projectTeamId)
                .orElseThrow(() -> AgileGuardException.notFound("ProjectTeam", projectTeamId));
        if (StringUtils.hasText(req.getName()))           team.setName(req.getName());
        if (StringUtils.hasText(req.getJiraProjectKey())) team.setJiraProjectKey(req.getJiraProjectKey());
        if (StringUtils.hasText(req.getGithubOrg()))      team.setGithubOrg(req.getGithubOrg());
        return projectTeamRepository.save(team);
    }

    /** Soft-deletes a project team. */
    @Transactional
    public void deactivateProjectTeam(String projectTeamId) {
        ProjectTeam team = projectTeamRepository.findById(projectTeamId)
                .orElseThrow(() -> AgileGuardException.notFound("ProjectTeam", projectTeamId));
        team.setActive(false);
        projectTeamRepository.save(team);
    }

    // ── Feature Team CRUD ─────────────────────────────────────────────────────

    /** Updates a feature team's name, JIRA component, or GitHub repos. */
    @Transactional
    public FeatureTeam updateFeatureTeam(String featureTeamId, FeatureTeamRequest req) {
        FeatureTeam team = featureTeamRepository.findById(featureTeamId)
                .orElseThrow(() -> AgileGuardException.notFound("FeatureTeam", featureTeamId));
        if (StringUtils.hasText(req.getName()))        team.setName(req.getName());
        if (StringUtils.hasText(req.getJiraComponent()))team.setJiraComponent(req.getJiraComponent());
        if (StringUtils.hasText(req.getGithubRepos())) team.setGithubRepos(req.getGithubRepos());
        return featureTeamRepository.save(team);
    }

    /** Soft-deletes a feature team. */
    @Transactional
    public void deactivateFeatureTeam(String featureTeamId) {
        FeatureTeam team = featureTeamRepository.findById(featureTeamId)
                .orElseThrow(() -> AgileGuardException.notFound("FeatureTeam", featureTeamId));
        team.setActive(false);
        featureTeamRepository.save(team);
    }

    // ── User management ───────────────────────────────────────────────────────

    /** Returns all users belonging to a tenant as safe response projections. */
    @Transactional(readOnly = true)
    public List<UserResponse> listUsersInTenant(String tenantId) {
        requireTenant(tenantId);
        return userRepository.findAllByTenantId(tenantId).stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    /** Returns a single user as a safe projection. */
    @Transactional(readOnly = true)
    public UserResponse getUser(String userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> AgileGuardException.notFound("User", userId));
        return toUserResponse(user);
    }

    /**
     * Updates a user's role and optionally moves them to a different feature team.
     * Passing featureTeamId = null removes the user from their current team.
     */
    @Transactional
    public UserResponse updateUserRole(String userId, UpdateUserRoleRequest req) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> AgileGuardException.notFound("User", userId));

        user.setRole(req.getRole());

        if (req.getFeatureTeamId() != null) {
            FeatureTeam ft = featureTeamRepository.findById(req.getFeatureTeamId())
                    .orElseThrow(() -> AgileGuardException.notFound("FeatureTeam",
                            req.getFeatureTeamId()));
            user.setFeatureTeam(ft);
        } else {
            user.setFeatureTeam(null);
        }

        log.info("Updated role for user '{}' → {}", user.getEmail(), req.getRole());
        return toUserResponse(userRepository.save(user));
    }

    /** Deactivates a user (soft delete). */
    @Transactional
    public void deactivateUser(String userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> AgileGuardException.notFound("User", userId));
        user.setActive(false);
        userRepository.save(user);
        log.info("Deactivated user '{}'", user.getEmail());
    }

    // ── JIRA connection test ──────────────────────────────────────────────────

    /**
     * Validates JIRA credentials by calling the Atlassian myself endpoint.
     * Returns connection status and the list of accessible project keys.
     *
     * In local/dev (mock) mode, returns a successful mock response.
     * In real mode, makes an HTTP call to the JIRA REST API.
     */
    public JiraConnectionTestResponse testJiraConnection(JiraConnectionTestRequest req) {
        // For POC — always succeeds with mock data.
        // Replace with a real WebClient call to req.getBaseUrl() + "/rest/api/3/myself"
        // using Basic Auth (email:apiToken) in dev/uat/prod.
        log.info("Testing JIRA connection to: {}", req.getBaseUrl());
        return JiraConnectionTestResponse.builder()
                .connected(true)
                .message("Connection successful")
                .jiraAccountId("jira-acc-mock-001")
                .displayName("AgileGuard Service Account")
                .accessibleProjects(List.of("COMMSSURV", "SHOP", "INFRA"))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Tenant requireTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> AgileGuardException.notFound("Tenant", tenantId));
    }

    private UserResponse toUserResponse(AppUser u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .role(u.getRole())
                .tenantId(u.getTenant() != null ? u.getTenant().getId() : null)
                .tenantName(u.getTenant() != null ? u.getTenant().getName() : null)
                .featureTeamId(u.getFeatureTeam() != null ? u.getFeatureTeam().getId() : null)
                .featureTeamName(u.getFeatureTeam() != null ? u.getFeatureTeam().getName() : null)
                .projectTeamId(u.getFeatureTeam() != null &&
                               u.getFeatureTeam().getProjectTeam() != null
                               ? u.getFeatureTeam().getProjectTeam().getId() : null)
                .projectTeamName(u.getFeatureTeam() != null &&
                                 u.getFeatureTeam().getProjectTeam() != null
                                 ? u.getFeatureTeam().getProjectTeam().getName() : null)
                .githubUsername(u.getGithubUsername())
                .jiraAccountId(u.getJiraAccountId())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
