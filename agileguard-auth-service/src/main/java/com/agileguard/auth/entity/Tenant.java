package com.agileguard.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an organisation (tenant) in the multi-tenant AgileGuard platform.
 * Each tenant has its own JIRA workspace and isolated data.
 */
@Entity
@Table(name = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    /** Base URL of the tenant's Atlassian JIRA instance. */
    private String jiraBaseUrl;

    /** JIRA API token (in production this would be a Secret Manager reference). */
    private String jiraApiToken;

    /** JIRA user email for API calls. */
    private String jiraUserEmail;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Plan plan = Plan.FREE;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProjectTeam> projects = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /** Subscription plan for the tenant. */
    public enum Plan { FREE, PRO, ENTERPRISE }
}
