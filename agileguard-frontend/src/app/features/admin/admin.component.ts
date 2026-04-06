import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TenantService } from '../../core/services/tenant.service';
import { AuthService } from '../../core/services/auth.service';
import {
  Tenant, ProjectTeam, FeatureTeam, UserResponse, UpdateTenantRequest
} from '../../core/models';

/**
 * Admin panel — tenant management dashboard.
 *
 * Sections:
 *   Overview    — tenant details, plan, JIRA status
 *   Projects    — list / create / edit project teams
 *   Teams       — list / create / edit feature teams (per selected project)
 *   Members     — list users, update roles, deactivate
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  template: `
    <div>

      <!-- ── Page Header ───────────────────────────────────────────── -->
      <div class="page-header flex justify-between items-center" style="flex-wrap:wrap;gap:12px">
        <div>
          <h1 class="page-title">⚙️ Admin Panel</h1>
          <p class="page-subtitle">
            Manage your tenant hierarchy, teams, and user roles
            <span *ngIf="welcomeMode()" class="badge badge-success" style="margin-left:8px">
              🎉 Welcome to AgileGuard!
            </span>
          </p>
        </div>
        <a routerLink="/onboarding" class="btn btn-secondary btn-sm">+ New Organisation</a>
      </div>

      <!-- ── Welcome banner ────────────────────────────────────────── -->
      <div *ngIf="welcomeMode()" class="alert alert-success mb-24">
        <strong>🎉 Organisation created successfully!</strong>
        Your workspace is ready. Configure your JIRA connection and invite team members below.
      </div>

      <!-- ── Tabs ──────────────────────────────────────────────────── -->
      <div class="tab-bar mb-24">
        <button *ngFor="let tab of tabs" class="tab-btn"
                [class.active]="activeTab() === tab.key"
                (click)="activeTab.set(tab.key)">
          {{ tab.icon }} {{ tab.label }}
        </button>
      </div>

      <!-- Loading -->
      <div *ngIf="loading()" style="text-align:center;padding:48px">
        <div class="spinner" style="width:36px;height:36px;margin:0 auto 12px"></div>
        <p class="text-secondary">Loading…</p>
      </div>

      <!-- ══ Overview tab ════════════════════════════════════════════ -->
      <ng-container *ngIf="!loading() && activeTab() === 'overview' && tenant() as t">
        <div class="grid-2">

          <div class="card">
            <div class="card-title">Organisation Details</div>
            <ng-container *ngIf="!editingOrg(); else editOrgForm">
              <div class="detail-row"><span>Name</span><strong>{{ t.name }}</strong></div>
              <div class="detail-row"><span>Slug</span><code>{{ t.slug }}</code></div>
              <div class="detail-row"><span>Plan</span>
                <span class="badge badge-purple">{{ t.plan }}</span>
              </div>
              <div class="detail-row"><span>Status</span>
                <span class="badge" [ngClass]="t.active ? 'badge-success' : 'badge-danger'">
                  {{ t.active ? 'Active' : 'Inactive' }}
                </span>
              </div>
              <button class="btn btn-secondary btn-sm mt-8" (click)="startEditOrg()">
                ✏️ Edit Details
              </button>
            </ng-container>
            <ng-template #editOrgForm>
              <form [formGroup]="orgEditForm" (ngSubmit)="saveOrg()">
                <div class="form-group">
                  <label class="form-label">Name</label>
                  <input class="form-control" formControlName="name">
                </div>
                <div class="form-group">
                  <label class="form-label">Plan</label>
                  <select class="form-control" formControlName="plan">
                    <option *ngFor="let p of ['FREE','PRO','ENTERPRISE']" [value]="p">{{ p }}</option>
                  </select>
                </div>
                <div class="flex gap-8 mt-8">
                  <button class="btn btn-primary btn-sm" type="submit" [disabled]="savingOrg()">
                    {{ savingOrg() ? 'Saving…' : '💾 Save' }}
                  </button>
                  <button class="btn btn-secondary btn-sm" type="button" (click)="editingOrg.set(false)">
                    Cancel
                  </button>
                </div>
              </form>
            </ng-template>
          </div>

          <div class="card">
            <div class="card-title">🔗 JIRA Configuration</div>
            <ng-container *ngIf="!editingJira(); else editJiraForm">
              <div class="detail-row"><span>Base URL</span>
                <strong>{{ t.jiraBaseUrl || '—' }}</strong>
              </div>
              <div class="detail-row"><span>Service Account</span>
                <strong>{{ t.jiraUserEmail || '—' }}</strong>
              </div>
              <div class="detail-row"><span>API Token</span>
                <span *ngIf="t.jiraApiToken" class="text-muted">••••••••••</span>
                <span *ngIf="!t.jiraApiToken" class="text-muted">Not configured</span>
              </div>
              <button class="btn btn-secondary btn-sm mt-8" (click)="editingJira.set(true)">
                ✏️ Update JIRA Settings
              </button>
            </ng-container>
            <ng-template #editJiraForm>
              <form [formGroup]="jiraEditForm" (ngSubmit)="saveJira()">
                <div class="form-group">
                  <label class="form-label">Base URL</label>
                  <input class="form-control" formControlName="jiraBaseUrl"
                         placeholder="https://your-org.atlassian.net">
                </div>
                <div class="form-group">
                  <label class="form-label">Service Account Email</label>
                  <input class="form-control" formControlName="jiraUserEmail">
                </div>
                <div class="form-group">
                  <label class="form-label">API Token</label>
                  <input class="form-control" type="password" formControlName="jiraApiToken"
                         placeholder="Leave blank to keep current token">
                </div>
                <div class="flex gap-8 mt-8">
                  <button class="btn btn-primary btn-sm" type="submit" [disabled]="savingOrg()">
                    {{ savingOrg() ? 'Saving…' : '💾 Save' }}
                  </button>
                  <button class="btn btn-secondary btn-sm" type="button" (click)="editingJira.set(false)">
                    Cancel
                  </button>
                </div>
              </form>
            </ng-template>
          </div>
        </div>

        <!-- Stats row -->
        <div class="stats-grid mt-16">
          <div class="stat-card">
            <div class="stat-value">{{ projects().length }}</div>
            <div class="stat-label">Project Teams</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ totalFeatureTeams() }}</div>
            <div class="stat-label">Feature Teams</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ members().length }}</div>
            <div class="stat-label">Team Members</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ activeMembers() }}</div>
            <div class="stat-label">Active Members</div>
          </div>
        </div>
      </ng-container>

      <!-- ══ Projects tab ════════════════════════════════════════════ -->
      <ng-container *ngIf="!loading() && activeTab() === 'projects'">

        <div class="card mb-16" *ngIf="showAddProject()">
          <div class="card-title">Add Project Team</div>
          <form [formGroup]="newProjectForm" (ngSubmit)="createProject()" class="grid-2" style="align-items:end;gap:16px">
            <div class="form-group" style="margin-bottom:0">
              <label class="form-label required">Team Name</label>
              <input class="form-control" formControlName="name" placeholder="Platform Team">
            </div>
            <div class="form-group" style="margin-bottom:0">
              <label class="form-label">JIRA Project Key</label>
              <input class="form-control" formControlName="jiraProjectKey" placeholder="COMMSSURV"
                     (input)="uppercaseKey($event, newProjectForm, 'jiraProjectKey')">
            </div>
            <div class="form-group" style="margin-bottom:0">
              <label class="form-label">GitHub Org</label>
              <input class="form-control" formControlName="githubOrg" placeholder="db-platform">
            </div>
            <div class="flex gap-8">
              <button class="btn btn-primary" type="submit" [disabled]="newProjectForm.invalid || savingProject()">
                {{ savingProject() ? 'Adding…' : '+ Add Project' }}
              </button>
              <button class="btn btn-secondary" type="button" (click)="showAddProject.set(false)">
                Cancel
              </button>
            </div>
          </form>
        </div>

        <div class="card">
          <div class="card-title flex justify-between items-center">
            <span>Project Teams ({{ projects().length }})</span>
            <button class="btn btn-primary btn-sm" (click)="showAddProject.set(!showAddProject())">
              + Add Project Team
            </button>
          </div>

          <div *ngIf="projects().length === 0" style="text-align:center;padding:32px;color:var(--text-secondary)">
            No project teams yet. Click "+ Add Project Team" to get started.
          </div>

          <table class="table" *ngIf="projects().length > 0">
            <thead>
              <tr><th>Name</th><th>JIRA Key</th><th>GitHub Org</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of projects()">
                <td style="font-weight:500">{{ p.name }}</td>
                <td><code *ngIf="p.jiraProjectKey">{{ p.jiraProjectKey }}</code>
                    <span *ngIf="!p.jiraProjectKey" class="text-muted text-xs">—</span></td>
                <td class="text-sm text-secondary">{{ p.githubOrg || '—' }}</td>
                <td><span class="badge badge-success">Active</span></td>
                <td>
                  <div class="flex gap-4">
                    <button class="btn btn-secondary btn-sm"
                            (click)="selectProject(p)">👥 Teams</button>
                    <button class="btn btn-danger btn-sm"
                            (click)="confirmDeactivateProject(p.id)">✕</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </ng-container>

      <!-- ══ Feature Teams tab ════════════════════════════════════════ -->
      <ng-container *ngIf="!loading() && activeTab() === 'teams'">

        <div class="flex items-center gap-12 mb-16">
          <label class="form-label" style="margin:0;white-space:nowrap">Select Project:</label>
          <select class="form-control" style="max-width:280px"
                  [ngModel]="selectedProjectId()"
                  (ngModelChange)="onProjectSelected($event)">
            <option value="">— Choose a project team —</option>
            <option *ngFor="let p of projects()" [value]="p.id">{{ p.name }}</option>
          </select>
        </div>

        <ng-container *ngIf="selectedProjectId()">

          <div class="card mb-16" *ngIf="showAddTeam()">
            <div class="card-title">Add Feature Team</div>
            <form [formGroup]="newTeamForm" (ngSubmit)="createTeam()" class="grid-2" style="align-items:end;gap:16px">
              <div class="form-group" style="margin-bottom:0">
                <label class="form-label required">Team Name</label>
                <input class="form-control" formControlName="name" placeholder="Payments Team">
              </div>
              <div class="form-group" style="margin-bottom:0">
                <label class="form-label">JIRA Component</label>
                <input class="form-control" formControlName="jiraComponent" placeholder="Payments">
              </div>
              <div class="form-group" style="margin-bottom:0">
                <label class="form-label">GitHub Repos (comma-separated)</label>
                <input class="form-control" formControlName="githubRepos" placeholder="payments-api,checkout">
              </div>
              <div class="flex gap-8">
                <button class="btn btn-primary" type="submit"
                        [disabled]="newTeamForm.invalid || savingTeam()">
                  {{ savingTeam() ? 'Adding…' : '+ Add Team' }}
                </button>
                <button class="btn btn-secondary" type="button" (click)="showAddTeam.set(false)">Cancel</button>
              </div>
            </form>
          </div>

          <div class="card">
            <div class="card-title flex justify-between items-center">
              <span>Feature Teams — {{ selectedProjectName() }} ({{ featureTeams().length }})</span>
              <button class="btn btn-primary btn-sm" (click)="showAddTeam.set(!showAddTeam())">
                + Add Feature Team
              </button>
            </div>
            <div *ngIf="featureTeams().length === 0"
                 style="text-align:center;padding:32px;color:var(--text-secondary)">
              No feature teams in this project yet.
            </div>
            <table class="table" *ngIf="featureTeams().length > 0">
              <thead>
                <tr><th>Name</th><th>JIRA Component</th><th>GitHub Repos</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                <tr *ngFor="let t of featureTeams()">
                  <td style="font-weight:500">{{ t.name }}</td>
                  <td><span *ngIf="t.jiraComponent" class="badge badge-info">{{ t.jiraComponent }}</span>
                      <span *ngIf="!t.jiraComponent" class="text-muted text-xs">—</span></td>
                  <td class="text-sm text-secondary" style="font-family:monospace;font-size:11px">
                    {{ t.githubRepos || '—' }}
                  </td>
                  <td><span class="badge badge-success">Active</span></td>
                  <td>
                    <button class="btn btn-danger btn-sm"
                            (click)="confirmDeactivateTeam(t.id)">✕</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </ng-container>

        <div *ngIf="!selectedProjectId()" class="card"
             style="text-align:center;padding:48px;color:var(--text-secondary)">
          <div style="font-size:36px;margin-bottom:12px">👥</div>
          Select a project team above to manage its feature teams.
        </div>
      </ng-container>

      <!-- ══ Members tab ═════════════════════════════════════════════ -->
      <ng-container *ngIf="!loading() && activeTab() === 'members'">
        <div class="card">
          <div class="card-title flex justify-between items-center">
            <span>Team Members ({{ members().length }})</span>
            <a routerLink="/login" class="btn btn-primary btn-sm"
               style="text-decoration:none">+ Invite Member</a>
          </div>

          <div *ngIf="members().length === 0"
               style="text-align:center;padding:32px;color:var(--text-secondary)">
            No members yet. Invite your first team member.
          </div>

          <table class="table" *ngIf="members().length > 0">
            <thead>
              <tr>
                <th>Member</th><th>Role</th><th>Team</th>
                <th>GitHub</th><th>Status</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let m of members()">
                <td>
                  <div style="font-weight:500">{{ m.fullName }}</div>
                  <div style="font-size:12px;color:var(--text-muted)">{{ m.email }}</div>
                </td>
                <td>
                  <select class="form-control" style="font-size:12px;padding:4px 8px;width:auto"
                          [ngModel]="m.role"
                          (ngModelChange)="updateRole(m.id, $event, m.featureTeamId)">
                    <option *ngFor="let r of roles" [value]="r">{{ r }}</option>
                  </select>
                </td>
                <td class="text-sm text-secondary">{{ m.featureTeamName || '—' }}</td>
                <td class="text-sm">
                  <span *ngIf="m.githubUsername"
                        style="font-family:monospace">&#64;{{ m.githubUsername }}</span>
                  <span *ngIf="!m.githubUsername" class="text-muted text-xs">—</span>
                </td>
                <td>
                  <span class="badge"
                        [ngClass]="m.active ? 'badge-success' : 'badge-danger'">
                    {{ m.active ? 'Active' : 'Inactive' }}
                  </span>
                </td>
                <td>
                  <button class="btn btn-danger btn-sm"
                          *ngIf="m.active && m.id !== auth.currentUser()?.userId"
                          (click)="confirmDeactivateUser(m.id)">✕</button>
                  <span *ngIf="m.id === auth.currentUser()?.userId"
                        class="text-muted text-xs">(you)</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </ng-container>

    </div>
  `,
  styles: [`
    .tab-bar { display:flex;gap:4px;border-bottom:1px solid var(--border);padding-bottom:0; }
    .tab-btn { padding:10px 18px;border:none;background:transparent;font-size:14px;
               font-weight:500;color:var(--text-secondary);cursor:pointer;
               border-bottom:2px solid transparent;margin-bottom:-1px;transition:all .15s; }
    .tab-btn:hover { color:var(--text-primary); }
    .tab-btn.active { color:var(--primary);border-bottom-color:var(--primary); }
    .detail-row { display:flex;align-items:center;justify-content:space-between;
                  padding:10px 0;border-bottom:1px solid var(--border);font-size:13px; }
    .detail-row:last-of-type { border-bottom:none; }
    .detail-row > span:first-child { color:var(--text-secondary);min-width:120px; }
  `]
})
export class AdminComponent implements OnInit {

  tabs = [
    { key: 'overview',  icon: '🏢', label: 'Overview'  },
    { key: 'projects',  icon: '📁', label: 'Projects'  },
    { key: 'teams',     icon: '👥', label: 'Teams'     },
    { key: 'members',   icon: '👤', label: 'Members'   },
  ];

  roles = ['SUPER_ADMIN','TENANT_ADMIN','PROJECT_ADMIN','FEATURE_LEAD',
           'PRODUCT_OWNER','DEVELOPER','QA_TESTER','VIEWER'];

  activeTab         = signal('overview');
  loading           = signal(true);
  welcomeMode       = signal(false);
  editingOrg        = signal(false);
  editingJira       = signal(false);
  savingOrg         = signal(false);
  savingProject     = signal(false);
  savingTeam        = signal(false);
  showAddProject    = signal(false);
  showAddTeam       = signal(false);
  selectedProjectId = signal('');

  tenant         = signal<Tenant | null>(null);
  projects       = signal<ProjectTeam[]>([]);
  featureTeams   = signal<FeatureTeam[]>([]);
  members        = signal<UserResponse[]>([]);

  totalFeatureTeams = computed(() => this.featureTeams().length);
  activeMembers     = computed(() => this.members().filter(m => m.active).length);
  selectedProjectName = computed(() =>
    this.projects().find(p => p.id === this.selectedProjectId())?.name ?? '');

  orgEditForm  = this.fb.group({ name: [''], plan: ['FREE'] });
  jiraEditForm = this.fb.group({ jiraBaseUrl: [''], jiraUserEmail: [''], jiraApiToken: [''] });
  newProjectForm = this.fb.group({
    name: ['', Validators.required], jiraProjectKey: [''], githubOrg: ['']
  });
  newTeamForm = this.fb.group({
    name: ['', Validators.required], jiraComponent: [''], githubRepos: ['']
  });

  constructor(
    private tenantSvc: TenantService,
    public  auth: AuthService,
    private route: ActivatedRoute,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['welcome']) this.welcomeMode.set(true);
      const tenantId = params['tenantId'] ?? this.auth.getTenantId();
      if (tenantId) this.loadAll(tenantId);
    });
  }

  loadAll(tenantId: string): void {
    this.loading.set(true);
    this.tenantSvc.getTenant(tenantId).subscribe({
      next: res => {
        this.tenant.set(res.data);
        this.orgEditForm.patchValue({ name: res.data.name, plan: res.data.plan });
        this.jiraEditForm.patchValue({ jiraBaseUrl: res.data.jiraBaseUrl ?? '', jiraUserEmail: res.data.jiraUserEmail ?? '' });
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
    this.tenantSvc.listProjects(tenantId).subscribe({ next: r => this.projects.set(r.data), error: () => {} });
    this.tenantSvc.listUsers(tenantId).subscribe({ next: r => this.members.set(r.data), error: () => {} });
  }

  startEditOrg(): void { this.editingOrg.set(true); }

  saveOrg(): void {
    const t = this.tenant(); if (!t) return;
    this.savingOrg.set(true);
    const v = this.orgEditForm.value;
    const req: UpdateTenantRequest = {
      ...(v.name  != null ? { name: v.name }   : {}),
      ...(v.plan  != null ? { plan: v.plan }   : {}),
    };
    this.tenantSvc.updateTenant(t.id, req).subscribe({
      next: r => { this.tenant.set(r.data); this.editingOrg.set(false); this.savingOrg.set(false); },
      error: () => this.savingOrg.set(false)
    });
  }

  saveJira(): void {
    const t = this.tenant(); if (!t) return;
    this.savingOrg.set(true);
    const v = this.jiraEditForm.value;
    const req: any = { jiraBaseUrl: v.jiraBaseUrl, jiraUserEmail: v.jiraUserEmail };
    if (v.jiraApiToken) req.jiraApiToken = v.jiraApiToken;
    this.tenantSvc.updateTenant(t.id, req).subscribe({
      next: r => { this.tenant.set(r.data); this.editingJira.set(false); this.savingOrg.set(false); },
      error: () => this.savingOrg.set(false)
    });
  }

  createProject(): void {
    const t = this.tenant(); if (!t) return;
    this.savingProject.set(true);
    const v = this.newProjectForm.value;
    const req: Partial<ProjectTeam> = {
      ...(v.name           != null ? { name:           v.name }           : {}),
      ...(v.jiraProjectKey != null ? { jiraProjectKey: v.jiraProjectKey } : {}),
      ...(v.githubOrg      != null ? { githubOrg:      v.githubOrg }      : {}),
    };
    this.tenantSvc.createProject(t.id, req).subscribe({
      next: r => {
        this.projects.update(ps => [...ps, r.data]);
        this.newProjectForm.reset();
        this.showAddProject.set(false);
        this.savingProject.set(false);
      },
      error: () => this.savingProject.set(false)
    });
  }

  selectProject(p: ProjectTeam): void {
    this.selectedProjectId.set(p.id);
    this.activeTab.set('teams');
    this.tenantSvc.listFeatureTeams(p.id).subscribe({ next: r => this.featureTeams.set(r.data) });
  }

  onProjectSelected(id: string): void {
    this.selectedProjectId.set(id);
    if (id) this.tenantSvc.listFeatureTeams(id).subscribe({ next: r => this.featureTeams.set(r.data) });
  }

  createTeam(): void {
    const pid = this.selectedProjectId(); if (!pid) return;
    this.savingTeam.set(true);
    const v = this.newTeamForm.value;
    const req: Partial<FeatureTeam> = {
      ...(v.name          != null ? { name:          v.name }          : {}),
      ...(v.jiraComponent != null ? { jiraComponent: v.jiraComponent } : {}),
      ...(v.githubRepos   != null ? { githubRepos:   v.githubRepos }   : {}),
    };
    this.tenantSvc.createFeatureTeam(pid, req).subscribe({
      next: r => {
        this.featureTeams.update(ts => [...ts, r.data]);
        this.newTeamForm.reset();
        this.showAddTeam.set(false);
        this.savingTeam.set(false);
      },
      error: () => this.savingTeam.set(false)
    });
  }

  updateRole(userId: string, role: string, featureTeamId?: string): void {
    this.tenantSvc.updateUserRole(userId, { role, featureTeamId }).subscribe({
      next: r => this.members.update(ms => ms.map(m => m.id === userId ? r.data : m))
    });
  }

  confirmDeactivateProject(id: string): void {
    if (!confirm('Deactivate this project team?')) return;
    this.tenantSvc.deactivateProject(id).subscribe({
      next: () => this.projects.update(ps => ps.filter(p => p.id !== id))
    });
  }

  confirmDeactivateTeam(id: string): void {
    if (!confirm('Deactivate this feature team?')) return;
    this.tenantSvc.deactivateFeatureTeam(id).subscribe({
      next: () => this.featureTeams.update(ts => ts.filter(t => t.id !== id))
    });
  }

  confirmDeactivateUser(id: string): void {
    if (!confirm('Deactivate this user? They will no longer be able to log in.')) return;
    this.tenantSvc.deactivateUser(id).subscribe({
      next: () => this.members.update(ms => ms.map(m => m.id === id ? { ...m, active: false } : m))
    });
  }

  uppercaseKey(event: Event, form: FormGroup, field: string): void {
    const v = (event.target as HTMLInputElement).value.toUpperCase();
    (event.target as HTMLInputElement).value = v;
    form.get(field)?.setValue(v, { emitEvent: false });
  }
}
