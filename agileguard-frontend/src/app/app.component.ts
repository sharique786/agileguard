import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';
import { AuthService }       from './core/services/auth.service';
import { TenantService }     from './core/services/tenant.service';
import { JiraConfigService } from './core/services/jira-config.service';

/**
 * Root component — rendered once for the lifetime of the app.
 *
 * Responsibilities:
 *   1. Renders the sidebar nav and <router-outlet>.
 *   2. On startup (ngOnInit), seeds the JIRA base URL into JiraConfigService
 *      so that every page can render clickable JIRA links immediately,
 *      regardless of whether the user has visited the Admin panel.
 *
 * URL seeding priority:
 *   a. If localStorage already has 'ag_jira_base_url' (from a previous session),
 *      JiraConfigService.baseUrl is already populated — no API call needed.
 *   b. If not cached, fetch the current tenant via GET /api/admin/tenants/{id}
 *      and call jiraConfig.setBaseUrl(jiraBaseUrl) — which persists to localStorage.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet],
  template: `
    <div class="main-layout" *ngIf="auth.isLoggedIn(); else publicView">

      <nav class="sidebar">
        <div class="sidebar-brand">🛡️ AgileGuard</div>

        <a class="nav-item" routerLink="/dashboard"     routerLinkActive="active">
          <span class="nav-icon">📊</span> Dashboard
        </a>
        <a class="nav-item" routerLink="/stories"       routerLinkActive="active">
          <span class="nav-icon">📝</span> Story Editor
        </a>
        <a class="nav-item" routerLink="/reports"       routerLinkActive="active">
          <span class="nav-icon">📈</span> Gap Reports
        </a>
        <a class="nav-item" routerLink="/sprint-report" routerLinkActive="active">
          <span class="nav-icon">📋</span> Sprint Report
        </a>
        <a class="nav-item" routerLink="/leaderboard"   routerLinkActive="active">
          <span class="nav-icon">🏆</span> Leaderboard
        </a>

        <ng-container *ngIf="isAdmin()">
          <div style="height:1px;background:var(--border);margin:8px 0"></div>
          <div style="font-size:10px;font-weight:700;text-transform:uppercase;
                      letter-spacing:.07em;color:var(--text-muted);padding:4px 12px">
            Administration
          </div>
          <a class="nav-item" routerLink="/admin"       routerLinkActive="active">
            <span class="nav-icon">⚙️</span> Admin Panel
          </a>
          <a class="nav-item" routerLink="/onboarding"  routerLinkActive="active">
            <span class="nav-icon">🏢</span> New Organisation
          </a>
        </ng-container>

        <div style="flex:1"></div>

        <div style="padding:12px;border-top:1px solid var(--border)">
          <div style="font-size:13px;font-weight:600;overflow:hidden;
                      text-overflow:ellipsis;white-space:nowrap">
            {{ auth.currentUser()?.fullName }}
          </div>
          <div style="font-size:11px;color:var(--text-muted);margin-top:2px">
            {{ auth.currentUser()?.role }}
          </div>
          <button class="btn btn-secondary btn-sm mt-8 w-full"
                  (click)="auth.logout()">Sign out</button>
        </div>
      </nav>

      <main class="main-content">
        <router-outlet />
      </main>
    </div>

    <ng-template #publicView>
      <router-outlet />
    </ng-template>
  `
})
export class AppComponent implements OnInit {

  constructor(
    public  auth:       AuthService,
    private tenantSvc:  TenantService,
    private jiraConfig: JiraConfigService
  ) {}

  ngOnInit(): void {
    // Seed the JIRA base URL as early as possible so every page has clickable links.
    // If the user is already logged in (e.g. page refresh), do it immediately.
    if (this.auth.isLoggedIn()) {
      this.seedJiraBaseUrl();
    }
  }

  isAdmin(): boolean {
    return ['SUPER_ADMIN', 'TENANT_ADMIN', 'PROJECT_ADMIN'].includes(this.auth.getRole());
  }

  /**
   * Fetches the current tenant and stores its JIRA base URL in JiraConfigService.
   *
   * Short-circuits immediately if:
   *   - The URL is already in localStorage (loaded by JiraConfigService on boot)
   *   - tenantId is not available (user not yet fully authenticated)
   *
   * Failure is silent — JIRA links simply render as non-clickable badges until
   * the next successful fetch.
   */
  private seedJiraBaseUrl(): void {
    // Already configured (e.g. from localStorage) — no API call needed
    if (this.jiraConfig.isConfigured()) { return; }

    const tenantId = this.auth.getTenantId();
    if (!tenantId) { return; }

    this.tenantSvc.getTenant(tenantId).subscribe({
      next: r => {
        if (r?.data?.jiraBaseUrl) {
          this.jiraConfig.setBaseUrl(r.data.jiraBaseUrl);
        }
      },
      error: () => { /* non-blocking — links just stay non-clickable badges */ }
    });
  }
}
