package com.agileguard.auth.config;

import com.agileguard.auth.entity.AppUser;
import com.agileguard.auth.entity.FeatureTeam;
import com.agileguard.auth.entity.ProjectTeam;
import com.agileguard.auth.entity.Tenant;
import com.agileguard.auth.repository.FeatureTeamRepository;
import com.agileguard.auth.repository.ProjectTeamRepository;
import com.agileguard.auth.repository.TenantRepository;
import com.agileguard.auth.repository.UserRepository;
import com.agileguard.common.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the local H2 database with demo data on startup.
 * Only active in the 'local' profile. Creates:
 *   - 1 tenant (db Corp)
 *   - 1 project team, 2 feature teams
 *   - 1 admin + 1 developer user
 */
@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final FeatureTeamRepository featureTeamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (tenantRepository.existsBySlug("db")) {
            log.info("Demo data already seeded — skipping.");
            return;
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("Deutsche Bank")
                .slug("db")
                .jiraBaseUrl("https://db.atlassian.net")
                .jiraApiToken("demo-token-replace-me")
                .jiraUserEmail("admin@db.com")
                .build());

        ProjectTeam project = projectTeamRepository.save(ProjectTeam.builder()
                .tenant(tenant)
                .name("Platform Team")
                .jiraProjectKey("COMMSSURV")
                .githubOrg("db-platform")
                .build());

        FeatureTeam payTeam = featureTeamRepository.save(FeatureTeam.builder()
                .projectTeam(project)
                .name("Payments Feature Team")
                .jiraComponent("Payments")
                .githubRepos("payments-service,checkout-api")
                .build());

        featureTeamRepository.save(FeatureTeam.builder()
                .projectTeam(project)
                .name("Auth Feature Team")
                .jiraComponent("Authentication")
                .githubRepos("auth-service")
                .build());

        // Admin user
        userRepository.save(AppUser.builder()
                .email("admin@db.com")
                .fullName("DB Admin")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(Role.PROJECT_ADMIN)
                .tenant(tenant)
                .githubUsername("db-admin")
                .build());

        // Developer user
        userRepository.save(AppUser.builder()
                .email("dev@db.com")
                .fullName("Esha Basu")
                .password(passwordEncoder.encode("Dev@1234"))
                .role(Role.DEVELOPER)
                .tenant(tenant)
                .featureTeam(payTeam)
                .githubUsername("esha-dev")
                .jiraAccountId("jira-acc-001")
                .build());

        // QA user
        userRepository.save(AppUser.builder()
                .email("qa@db.com")
                .fullName("Sachin Tester")
                .password(passwordEncoder.encode("Qa@1234"))
                .role(Role.QA_TESTER)
                .tenant(tenant)
                .featureTeam(payTeam)
                .githubUsername("sachin-qa")
                .jiraAccountId("jira-acc-002")
                .build());

        log.info("=======================================================");
        log.info("Demo data seeded successfully!");
        log.info("  Tenant: Deutsche Bank (id will be shown in H2 console)");
        log.info("  Admin login:  admin@db.com / Admin@1234");
        log.info("  Dev login:    dev@db.com / Dev@1234");
        log.info("  QA login:     qa@db.com / Qa@1234");
        log.info("  H2 Console:   http://localhost:8081/h2-console");
        log.info("=======================================================");
    }
}
