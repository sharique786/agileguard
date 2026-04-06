package com.agileguard.auth.repository;

import com.agileguard.auth.entity.ProjectTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** JPA repository for ProjectTeam. */
@Repository
public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, String> {
    List<ProjectTeam> findAllByTenantId(String tenantId);
    boolean existsByTenantIdAndJiraProjectKey(String tenantId, String jiraProjectKey);
}
