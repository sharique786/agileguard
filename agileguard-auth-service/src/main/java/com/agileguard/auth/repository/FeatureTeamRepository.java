package com.agileguard.auth.repository;

import com.agileguard.auth.entity.FeatureTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** JPA repository for FeatureTeam. */
@Repository
public interface FeatureTeamRepository extends JpaRepository<FeatureTeam, String> {
    List<FeatureTeam> findAllByProjectTeamId(String projectTeamId);
}
