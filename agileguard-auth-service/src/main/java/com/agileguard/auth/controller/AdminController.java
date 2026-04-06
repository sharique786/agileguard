package com.agileguard.auth.controller;

import com.agileguard.auth.dto.FeatureTeamRequest;
import com.agileguard.auth.dto.ProjectTeamRequest;
import com.agileguard.auth.dto.UpdateTenantRequest;
import com.agileguard.auth.dto.UpdateUserRoleRequest;
import com.agileguard.auth.dto.UserResponse;
import com.agileguard.auth.entity.FeatureTeam;
import com.agileguard.auth.entity.ProjectTeam;
import com.agileguard.auth.entity.Tenant;
import com.agileguard.auth.service.TenantManagementService;
import com.agileguard.auth.service.TenantService;
import com.agileguard.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin REST controller — full CRUD for tenant hierarchy management.
 * All endpoints require TENANT_ADMIN, PROJECT_ADMIN, or SUPER_ADMIN.
 *
 * Tenant management:
 *   GET    /api/admin/tenants/{id}              — tenant details
 *   PUT    /api/admin/tenants/{id}              — update tenant
 *   DELETE /api/admin/tenants/{id}              — deactivate tenant
 *
 * Project Team management:
 *   GET    /api/admin/tenants/{id}/projects     — list project teams
 *   POST   /api/admin/tenants/{id}/projects     — create project team
 *   PUT    /api/admin/projects/{id}             — update project team
 *   DELETE /api/admin/projects/{id}             — deactivate project team
 *
 * Feature Team management:
 *   GET    /api/admin/projects/{id}/teams       — list feature teams
 *   POST   /api/admin/projects/{id}/teams       — create feature team
 *   PUT    /api/admin/teams/{id}               — update feature team
 *   DELETE /api/admin/teams/{id}               — deactivate feature team
 *
 * User management:
 *   GET    /api/admin/tenants/{id}/users        — list users in tenant
 *   GET    /api/admin/users/{id}               — get single user
 *   PUT    /api/admin/users/{id}/role           — update role / team
 *   DELETE /api/admin/users/{id}               — deactivate user
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TenantService          tenantService;
    private final TenantManagementService mgmtService;

    // ── Tenant ────────────────────────────────────────────────────────────────

    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<Tenant>> getTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getTenant(tenantId)));
    }

    @PutMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Tenant>> updateTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Tenant updated",
                mgmtService.updateTenant(tenantId, req)));
    }

    @DeleteMapping("/tenants/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateTenant(@PathVariable String tenantId) {
        mgmtService.deactivateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Tenant deactivated", null));
    }

    // ── Project Teams ─────────────────────────────────────────────────────────

    @GetMapping("/tenants/{tenantId}/projects")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProjectTeam>>> listProjects(@PathVariable String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.listProjectTeams(tenantId)));
    }

    @PostMapping("/tenants/{tenantId}/projects")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<ProjectTeam>> createProject(
            @PathVariable String tenantId,
            @Valid @RequestBody ProjectTeamRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Project team created",
                tenantService.createProjectTeam(tenantId, req)));
    }

    @PutMapping("/projects/{projectId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<ProjectTeam>> updateProject(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectTeamRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Project team updated",
                mgmtService.updateProjectTeam(projectId, req)));
    }

    @DeleteMapping("/projects/{projectId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateProject(@PathVariable String projectId) {
        mgmtService.deactivateProjectTeam(projectId);
        return ResponseEntity.ok(ApiResponse.success("Project team deactivated", null));
    }

    // ── Feature Teams ─────────────────────────────────────────────────────────

    @GetMapping("/projects/{projectId}/teams")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<List<FeatureTeam>>> listFeatureTeams(
            @PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.listFeatureTeams(projectId)));
    }

    @PostMapping("/projects/{projectId}/teams")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<FeatureTeam>> createFeatureTeam(
            @PathVariable String projectId,
            @Valid @RequestBody FeatureTeamRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Feature team created",
                tenantService.createFeatureTeam(projectId, req)));
    }

    @PutMapping("/teams/{teamId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<FeatureTeam>> updateFeatureTeam(
            @PathVariable String teamId,
            @Valid @RequestBody FeatureTeamRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Feature team updated",
                mgmtService.updateFeatureTeam(teamId, req)));
    }

    @DeleteMapping("/teams/{teamId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateFeatureTeam(@PathVariable String teamId) {
        mgmtService.deactivateFeatureTeam(teamId);
        return ResponseEntity.ok(ApiResponse.success("Feature team deactivated", null));
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @GetMapping("/tenants/{tenantId}/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(@PathVariable String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(mgmtService.listUsersInTenant(tenantId)));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(mgmtService.getUser(userId)));
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRoleRequest req) {
        return ResponseEntity.ok(ApiResponse.success("User role updated",
                mgmtService.updateUserRole(userId, req)));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable String userId) {
        mgmtService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", null));
    }
}
