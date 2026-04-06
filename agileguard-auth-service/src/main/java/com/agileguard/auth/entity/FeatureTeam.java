package com.agileguard.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a feature team under a project team.
 * Individual developers and QA testers are assigned to feature teams.
 */
@Entity
@Table(name = "feature_teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FeatureTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_team_id", nullable = false)
    @ToString.Exclude
    private ProjectTeam projectTeam;

    @Column(nullable = false)
    private String name;

    /** JIRA component name this team is responsible for. */
    private String jiraComponent;

    /** Comma-separated list of GitHub repository names. */
    private String githubRepos;

    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "featureTeam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AppUser> members = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;
}
