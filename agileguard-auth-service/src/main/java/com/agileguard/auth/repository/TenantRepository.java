package com.agileguard.auth.repository;

import com.agileguard.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** JPA repository for Tenant. */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
