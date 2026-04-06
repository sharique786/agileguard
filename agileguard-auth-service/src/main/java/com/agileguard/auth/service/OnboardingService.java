package com.agileguard.auth.service;

import com.agileguard.auth.dto.*;
import com.agileguard.auth.entity.*;
import com.agileguard.auth.repository.*;
import com.agileguard.common.enums.Role;
import com.agileguard.common.exception.AgileGuardException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the complete tenant onboarding workflow in a single
 * database transaction. On any failure the entire operation rolls back,
 * leaving the system in a clean state.
 *
 * Onboarding sequence:
 *   1. Validate uniqueness of tenant slug and admin email
 *   2. Create Tenant
 *   3. Create Project Team under that tenant
 *   4. Create zero-or-more Feature Teams under the project
 *   5. Create the Tenant Admin user
 *   6. Issue JWT tokens so the admin is logged in immediately
 *   7. Return OnboardingResponse with all created entity IDs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final TenantRepository      tenantRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final FeatureTeamRepository featureTeamRepository;
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;

    /**
     * Executes the complete onboarding workflow atomically.
     *
     * @param request validated onboarding payload
     * @return summary with all created IDs and JWT tokens for the admin user
     */
    @Transactional
    public OnboardingResponse onboard(OnboardingRequest request) {
        log.info("Starting onboarding for organisation: '{}'", request.getOrganisation().getName());

        // ── 1. Uniqueness checks ─────────────────────────────────────────────
        String slug = request.getOrganisation().getSlug();
        if (tenantRepository.existsBySlug(slug)) {
            throw AgileGuardException.conflict(
                "Organisation slug '" + slug + "' is already taken. Please choose a different one.");
        }
        if (userRepository.existsByEmail(request.getAdminUser().getEmail())) {
            throw AgileGuardException.conflict(
                "Email '" + request.getAdminUser().getEmail() + "' is already registered.");
        }

        // ── 2. Create Tenant ─────────────────────────────────────────────────
        var org = request.getOrganisation();
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(org.getName())
                .slug(org.getSlug())
                .jiraBaseUrl(org.getJiraBaseUrl())
                .jiraUserEmail(org.getJiraUserEmail())
                .jiraApiToken(org.getJiraApiToken())
                .plan(parsePlan(org.getPlan()))
                .active(true)
                .build());
        log.info("Created tenant '{}' (id: {})", tenant.getName(), tenant.getId());

        // ── 3. Create Project Team ───────────────────────────────────────────
        var pt = request.getProjectTeam();
        ProjectTeam projectTeam = projectTeamRepository.save(ProjectTeam.builder()
                .tenant(tenant)
                .name(pt.getName())
                .jiraProjectKey(pt.getJiraProjectKey())
                .githubOrg(pt.getGithubOrg())
                .active(true)
                .build());
        log.info("Created project team '{}' (id: {})", projectTeam.getName(), projectTeam.getId());

        // ── 4. Create Feature Teams ──────────────────────────────────────────
        List<OnboardingResponse.FeatureTeamSummary> featureSummaries = new ArrayList<>();
        if (request.getFeatureTeams() != null) {
            for (var ft : request.getFeatureTeams()) {
                FeatureTeam featureTeam = featureTeamRepository.save(FeatureTeam.builder()
                        .projectTeam(projectTeam)
                        .name(ft.getName())
                        .jiraComponent(ft.getJiraComponent())
                        .githubRepos(ft.getGithubRepos())
                        .active(true)
                        .build());
                featureSummaries.add(OnboardingResponse.FeatureTeamSummary.builder()
                        .id(featureTeam.getId())
                        .name(featureTeam.getName())
                        .build());
                log.info("Created feature team '{}'", featureTeam.getName());
            }
        }

        // ── 5. Create Tenant Admin user ──────────────────────────────────────
        var admin = request.getAdminUser();
        AppUser adminUser = userRepository.save(AppUser.builder()
                .tenant(tenant)
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .password(passwordEncoder.encode(admin.getPassword()))
                .role(Role.TENANT_ADMIN)
                .githubUsername(admin.getGithubUsername())
                .jiraAccountId(admin.getJiraAccountId())
                .active(true)
                .build());
        log.info("Created tenant admin '{}' (id: {})", adminUser.getEmail(), adminUser.getId());

        // ── 6. Issue JWT tokens ──────────────────────────────────────────────
        AuthResponse tokens = AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(adminUser))
                .refreshToken(jwtService.generateRefreshToken(adminUser))
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(adminUser.getId())
                .email(adminUser.getEmail())
                .fullName(adminUser.getFullName())
                .role(adminUser.getRole())
                .tenantId(tenant.getId())
                .build();

        log.info("Onboarding complete for '{}' — tenant id: {}", tenant.getName(), tenant.getId());

        // ── 7. Return summary ────────────────────────────────────────────────
        return OnboardingResponse.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .tenantSlug(tenant.getSlug())
                .projectTeamId(projectTeam.getId())
                .projectTeamName(projectTeam.getName())
                .featureTeams(featureSummaries)
                .adminUserId(adminUser.getId())
                .adminEmail(adminUser.getEmail())
                .tokens(tokens)
                .build();
    }

    /** Converts a plan string to the Tenant.Plan enum safely. */
    private Tenant.Plan parsePlan(String plan) {
        if (!StringUtils.hasText(plan)) return Tenant.Plan.FREE;
        try {
            return Tenant.Plan.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Tenant.Plan.FREE;
        }
    }

    /** Returns true if the slug is not already registered. */
    @Transactional(readOnly = true)
    public boolean isSlugAvailable(String slug) {
        return !tenantRepository.existsBySlug(slug);
    }
}
