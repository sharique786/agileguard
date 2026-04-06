import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, OnboardingRequest, OnboardingResponse,
  JiraConnectionTestRequest, JiraConnectionTestResponse,
  Tenant, ProjectTeam, FeatureTeam,
  UserResponse, UpdateTenantRequest, UpdateUserRoleRequest
} from '../models';

/** Service for tenant onboarding and admin management API calls. */
@Injectable({ providedIn: 'root' })
export class TenantService {
  private base  = `${environment.apiUrl}`;
  private ob    = `${this.base}/onboarding`;
  private admin = `${this.base}/admin`;

  constructor(private http: HttpClient) {}

  // ── Onboarding ──────────────────────────────────────────────────────────

  /** Submits the full onboarding wizard payload. No auth token needed. */
  onboard(req: OnboardingRequest): Observable<ApiResponse<OnboardingResponse>> {
    return this.http.post<ApiResponse<OnboardingResponse>>(this.ob, req);
  }

  /** Tests JIRA credentials — call from step 2 before saving. */
  testJiraConnection(req: JiraConnectionTestRequest): Observable<ApiResponse<JiraConnectionTestResponse>> {
    return this.http.post<ApiResponse<JiraConnectionTestResponse>>(`${this.ob}/jira-test`, req);
  }

  /** Returns whether a slug is available. */
  checkSlug(slug: string): Observable<ApiResponse<boolean>> {
    return this.http.get<ApiResponse<boolean>>(`${this.ob}/check-slug/${slug}`);
  }

  // ── Tenants ─────────────────────────────────────────────────────────────

  listTenants(): Observable<ApiResponse<Tenant[]>> {
    return this.http.get<ApiResponse<Tenant[]>>(`${this.base}/tenants`);
  }

  getTenant(id: string): Observable<ApiResponse<Tenant>> {
    return this.http.get<ApiResponse<Tenant>>(`${this.admin}/tenants/${id}`);
  }

  updateTenant(id: string, req: UpdateTenantRequest): Observable<ApiResponse<Tenant>> {
    return this.http.put<ApiResponse<Tenant>>(`${this.admin}/tenants/${id}`, req);
  }

  deactivateTenant(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.admin}/tenants/${id}`);
  }

  // ── Project teams ────────────────────────────────────────────────────────

  listProjects(tenantId: string): Observable<ApiResponse<ProjectTeam[]>> {
    return this.http.get<ApiResponse<ProjectTeam[]>>(`${this.admin}/tenants/${tenantId}/projects`);
  }

  createProject(tenantId: string, req: Partial<ProjectTeam>): Observable<ApiResponse<ProjectTeam>> {
    return this.http.post<ApiResponse<ProjectTeam>>(`${this.admin}/tenants/${tenantId}/projects`, req);
  }

  updateProject(id: string, req: Partial<ProjectTeam>): Observable<ApiResponse<ProjectTeam>> {
    return this.http.put<ApiResponse<ProjectTeam>>(`${this.admin}/projects/${id}`, req);
  }

  deactivateProject(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.admin}/projects/${id}`);
  }

  // ── Feature teams ────────────────────────────────────────────────────────

  listFeatureTeams(projectId: string): Observable<ApiResponse<FeatureTeam[]>> {
    return this.http.get<ApiResponse<FeatureTeam[]>>(`${this.admin}/projects/${projectId}/teams`);
  }

  createFeatureTeam(projectId: string, req: Partial<FeatureTeam>): Observable<ApiResponse<FeatureTeam>> {
    return this.http.post<ApiResponse<FeatureTeam>>(`${this.admin}/projects/${projectId}/teams`, req);
  }

  updateFeatureTeam(id: string, req: Partial<FeatureTeam>): Observable<ApiResponse<FeatureTeam>> {
    return this.http.put<ApiResponse<FeatureTeam>>(`${this.admin}/teams/${id}`, req);
  }

  deactivateFeatureTeam(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.admin}/teams/${id}`);
  }

  // ── Users ────────────────────────────────────────────────────────────────

  listUsers(tenantId: string): Observable<ApiResponse<UserResponse[]>> {
    return this.http.get<ApiResponse<UserResponse[]>>(`${this.admin}/tenants/${tenantId}/users`);
  }

  updateUserRole(userId: string, req: UpdateUserRoleRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(`${this.admin}/users/${userId}/role`, req);
  }

  deactivateUser(userId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.admin}/users/${userId}`);
  }
}
