package com.agileguard.auth.controller;

import com.agileguard.auth.dto.FeatureTeamRequest;
import com.agileguard.auth.dto.ProjectTeamRequest;
import com.agileguard.auth.dto.TenantRequest;
import com.agileguard.auth.entity.FeatureTeam;
import com.agileguard.auth.entity.ProjectTeam;
import com.agileguard.auth.entity.Tenant;
import com.agileguard.auth.service.TenantService;
import com.agileguard.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing the tenant hierarchy:
 * tenants → project teams → feature teams.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // ── Tenants ───────────────────────────────────────────────────────────────

    @PostMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Tenant>> createTenant(
            @Valid @RequestBody TenantRequest request) {
        Tenant tenant = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tenant created", tenant));
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Tenant>>> listTenants() {
        return ResponseEntity.ok(ApiResponse.success(tenantService.listTenants()));
    }

    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<Tenant>> getTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getTenant(tenantId)));
    }

    // ── Project Teams ─────────────────────────────────────────────────────────

    @PostMapping("/tenants/{tenantId}/projects")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<ProjectTeam>> createProjectTeam(
            @PathVariable String tenantId,
            @Valid @RequestBody ProjectTeamRequest request) {
        ProjectTeam team = tenantService.createProjectTeam(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project team created", team));
    }

    @GetMapping("/tenants/{tenantId}/projects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProjectTeam>>> listProjectTeams(
            @PathVariable String tenantId) {
        return ResponseEntity.ok(
                ApiResponse.success(tenantService.listProjectTeams(tenantId)));
    }

    // ── Feature Teams ─────────────────────────────────────────────────────────

    @PostMapping("/projects/{projectId}/feature-teams")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<FeatureTeam>> createFeatureTeam(
            @PathVariable String projectId,
            @Valid @RequestBody FeatureTeamRequest request) {
        FeatureTeam team = tenantService.createFeatureTeam(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Feature team created", team));
    }

    @GetMapping("/projects/{projectId}/feature-teams")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FeatureTeam>>> listFeatureTeams(
            @PathVariable String projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(tenantService.listFeatureTeams(projectId)));
    }
}
