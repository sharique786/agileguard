package com.agileguard.jira.repository;

import com.agileguard.common.enums.Severity;
import com.agileguard.jira.entity.GapFindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repository for gap finding persistence. */
@Repository
public interface GapFindingRepository extends JpaRepository<GapFindingEntity, String> {

    List<GapFindingEntity> findByTenantIdAndProjectIdAndResolvedFalse(
            String tenantId, String projectId);

    List<GapFindingEntity> findByJiraIssueKeyAndResolvedFalse(String issueKey);

    List<GapFindingEntity> findBySeverityAndResolvedFalse(Severity severity);

    @Query("SELECT COUNT(g) FROM GapFindingEntity g WHERE g.tenantId = :tenantId AND g.resolved = false")
    long countOpenGapsByTenant(String tenantId);

    @Query("SELECT g FROM GapFindingEntity g WHERE g.tenantId = :tenantId " +
           "AND g.resolved = false ORDER BY g.severity, g.detectedAt DESC")
    List<GapFindingEntity> findOpenGapsByTenantOrderBySeverity(String tenantId);
}
