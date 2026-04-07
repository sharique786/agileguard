import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

/** Root application shell with role-aware sidebar navigation. */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet],
  template: `
    <div class="main-layout" *ngIf="auth.isLoggedIn(); else publicView">

      <!-- Sidebar -->
      <nav class="sidebar">
        <div class="sidebar-brand">🛡️ AgileGuard</div>

        <a class="nav-item" routerLink="/dashboard" routerLinkActive="active">
          <span class="nav-icon">📊</span> Dashboard
        </a>
        <a class="nav-item" routerLink="/stories" routerLinkActive="active">
          <span class="nav-icon">📝</span> Story Editor
        </a>
        <a class="nav-item" routerLink="/reports" routerLinkActive="active">
          <span class="nav-icon">📈</span> Gap Reports
        </a>
        <a class="nav-item" routerLink="/leaderboard" routerLinkActive="active">
          <span class="nav-icon">🏆</span> Leaderboard
        </a>

        <!-- Admin section — only for ADMIN roles -->
        <ng-container *ngIf="isAdmin()">
          <div style="height:1px;background:var(--border);margin:8px 0"></div>
          <div style="font-size:10px;font-weight:700;text-transform:uppercase;
                      letter-spacing:.07em;color:var(--text-muted);padding:4px 12px">
            Administration
          </div>
          <a class="nav-item" routerLink="/admin" routerLinkActive="active">
            <span class="nav-icon">⚙️</span> Admin Panel
          </a>
          <a class="nav-item" routerLink="/onboarding" routerLinkActive="active">
            <span class="nav-icon">🏢</span> New Organisation
          </a>
        </ng-container>

        <div style="flex:1"></div>

        <!-- User info footer -->
        <div style="padding:12px;border-top:1px solid var(--border)">
          <div style="font-size:13px;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
            {{ auth.currentUser()?.fullName }}
          </div>
          <div style="font-size:11px;color:var(--text-muted);margin-top:2px">
            {{ auth.currentUser()?.role }}
          </div>
          <div style="font-size:11px;color:var(--text-muted);margin-top:1px;overflow:hidden;
                      text-overflow:ellipsis;white-space:nowrap">
            {{ auth.currentUser()?.email }}
          </div>
          <button class="btn btn-secondary btn-sm mt-8 w-full"
                  (click)="auth.logout()">Sign out</button>
        </div>
      </nav>

      <!-- Main content area -->
      <main class="main-content">
        <router-outlet />
      </main>
    </div>

    <!-- Public pages (login, onboarding) use their own full-screen layout -->
    <ng-template #publicView>
      <router-outlet />
    </ng-template>
  `
})
export class AppComponent {
  constructor(public auth: AuthService) {}

  /** True for roles that can access the admin section. */
  isAdmin(): boolean {
    return ['SUPER_ADMIN', 'TENANT_ADMIN', 'PROJECT_ADMIN'].includes(this.auth.getRole());
  }
}
