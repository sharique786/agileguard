package com.agileguard.jira.entity;

import com.agileguard.common.enums.GapType;
import com.agileguard.common.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** JPA entity for persisting gap findings to the local H2 database. */
@Entity
@Table(name = "gap_findings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GapFindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String tenantId;
    private String projectId;
    private String featureTeamId;
    private String jiraIssueKey;
    private String sprintId;

    @Enumerated(EnumType.STRING)
    private GapType gapType;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private int qualityScore;

    @Column(length = 1000)
    private String details;

    @Column(length = 500)
    private String suggestedAction;

    @Builder.Default
    private boolean resolved = false;

    @CreationTimestamp
    private Instant detectedAt;

    private Instant resolvedAt;
}
