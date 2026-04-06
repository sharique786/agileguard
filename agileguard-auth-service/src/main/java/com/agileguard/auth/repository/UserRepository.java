package com.agileguard.auth.repository;

import com.agileguard.auth.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** JPA repository for AppUser. */
@Repository
public interface UserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AppUser> findAllByTenantId(String tenantId);

    List<AppUser> findAllByFeatureTeamId(String featureTeamId);
}
