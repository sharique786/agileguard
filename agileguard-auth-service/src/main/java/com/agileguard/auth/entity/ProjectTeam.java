package com.agileguard.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a top-level project team within a tenant.
 * Maps to a JIRA project key and contains multiple feature teams.
 */
@Entity
@Table(name = "project_teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @ToString.Exclude
    private Tenant tenant;

    @Column(nullable = false)
    private String name;

    /** Corresponding JIRA project key (e.g., "SHOP", "PLATFORM"). */
    private String jiraProjectKey;

    /** GitHub organisation for this project's repositories. */
    private String githubOrg;

    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "projectTeam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FeatureTeam> featureTeams = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;
}
