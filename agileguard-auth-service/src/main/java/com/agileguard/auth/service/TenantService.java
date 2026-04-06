package com.agileguard.auth.service;

import com.agileguard.auth.dto.FeatureTeamRequest;
import com.agileguard.auth.dto.ProjectTeamRequest;
import com.agileguard.auth.dto.TenantRequest;
import com.agileguard.auth.entity.FeatureTeam;
import com.agileguard.auth.entity.ProjectTeam;
import com.agileguard.auth.entity.Tenant;
import com.agileguard.auth.repository.FeatureTeamRepository;
import com.agileguard.auth.repository.ProjectTeamRepository;
import com.agileguard.auth.repository.TenantRepository;
import com.agileguard.common.exception.AgileGuardException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing tenant hierarchy: tenants, project teams, and feature teams.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final FeatureTeamRepository featureTeamRepository;

    /** Creates a new tenant organisation. */
    @Transactional
    public Tenant createTenant(TenantRequest request) {
        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw AgileGuardException.conflict("Tenant slug already in use: " + request.getSlug());
        }
        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .jiraBaseUrl(request.getJiraBaseUrl())
                .jiraApiToken(request.getJiraApiToken())
                .jiraUserEmail(request.getJiraUserEmail())
                .build();
        return tenantRepository.save(tenant);
    }

    /** Retrieves a tenant by ID, throws 404 if not found. */
    @Transactional(readOnly = true)
    public Tenant getTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> AgileGuardException.notFound("Tenant", tenantId));
    }

    /** Lists all tenants (SUPER_ADMIN only). */
    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    /** Creates a project team within a tenant. */
    @Transactional
    public ProjectTeam createProjectTeam(String tenantId, ProjectTeamRequest request) {
        Tenant tenant = getTenant(tenantId);
        if (request.getJiraProjectKey() != null &&
            projectTeamRepository.existsByTenantIdAndJiraProjectKey(tenantId, request.getJiraProjectKey())) {
            throw AgileGuardException.conflict(
                "JIRA project key already configured: " + request.getJiraProjectKey());
        }
        ProjectTeam team = ProjectTeam.builder()
                .tenant(tenant)
                .name(request.getName())
                .jiraProjectKey(request.getJiraProjectKey())
                .githubOrg(request.getGithubOrg())
                .build();
        return projectTeamRepository.save(team);
    }

    /** Lists all project teams for a tenant. */
    @Transactional(readOnly = true)
    public List<ProjectTeam> listProjectTeams(String tenantId) {
        return projectTeamRepository.findAllByTenantId(tenantId);
    }

    /** Creates a feature team under a project team. */
    @Transactional
    public FeatureTeam createFeatureTeam(String projectTeamId, FeatureTeamRequest request) {
        ProjectTeam projectTeam = projectTeamRepository.findById(projectTeamId)
                .orElseThrow(() -> AgileGuardException.notFound("ProjectTeam", projectTeamId));
        FeatureTeam team = FeatureTeam.builder()
                .projectTeam(projectTeam)
                .name(request.getName())
                .jiraComponent(request.getJiraComponent())
                .githubRepos(request.getGithubRepos())
                .build();
        return featureTeamRepository.save(team);
    }

    /** Lists all feature teams under a project team. */
    @Transactional(readOnly = true)
    public List<FeatureTeam> listFeatureTeams(String projectTeamId) {
        return featureTeamRepository.findAllByProjectTeamId(projectTeamId);
    }
}
