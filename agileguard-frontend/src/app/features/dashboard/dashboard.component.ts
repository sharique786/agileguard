import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { JiraService }    from '../../core/services/jira.service';
import { AuthService }    from '../../core/services/auth.service';
import { TokenService }   from '../../core/services/token.service';
import { JiraKeyComponent } from '../../shared/components/jira-key.component';
import { SprintHealthReport, GapReport } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, JiraKeyComponent],
  template: `
  <div>
    <div class="page-header flex justify-between items-center">
      <div>
        <h1 class="page-title">Sprint Health Dashboard</h1>
        <p class="page-subtitle">Real-time SDLC quality overview · Project: {{ projectKey }}</p>
      </div>
      <button class="btn btn-primary" (click)="runScan()" [disabled]="scanning()">
        <span *ngIf="scanning()" class="spinner" style="width:16px;height:16px"></span>
        {{ scanning() ? 'Scanning...' : '🔍 Run Gap Scan' }}
      </button>
    </div>

    <!-- ── Personal Integration Tokens ─────────────────────────────────── -->
    <div class="card mb-24">
      <div class="card-header-row">
        <div>
          <div class="card-title">🔑 My Personal Integration Tokens</div>
          <div class="card-subtitle">
            Tokens are stored for this browser session only and cleared when you close the tab.
            They are used to test your personal connectivity — never saved to any database.
          </div>
        </div>
        <button class="btn btn-secondary btn-sm" (click)="showTokens.set(!showTokens())"
                style="flex-shrink:0">
          {{ showTokens() ? '▲ Collapse' : '▼ Configure' }}
        </button>
      </div>

      <!-- Token status pills (always visible) -->
      <div class="token-status-row mt-12">
        <div class="token-status-pill"
             [class.pill-ok]="tokens.jiraStatus() === 'ok'"
             [class.pill-fail]="tokens.jiraStatus() === 'fail'"
             [class.pill-testing]="tokens.jiraStatus() === 'testing'"
             [class.pill-set]="tokens.hasJira() && tokens.jiraStatus() === 'idle'">
          <span class="pill-icon">{{ jiraStatusIcon() }}</span>
          JIRA
          <span *ngIf="tokens.jiraStatus() === 'ok'" class="pill-msg">{{ tokens.jiraMessage() }}</span>
          <span *ngIf="tokens.jiraStatus() === 'fail'" class="pill-msg-err">{{ tokens.jiraMessage() }}</span>
          <span *ngIf="!tokens.hasJira()" class="pill-msg-muted">not configured</span>
        </div>
        <div class="token-status-pill"
             [class.pill-ok]="tokens.githubStatus() === 'ok'"
             [class.pill-fail]="tokens.githubStatus() === 'fail'"
             [class.pill-testing]="tokens.githubStatus() === 'testing'"
             [class.pill-set]="tokens.hasGithub() && tokens.githubStatus() === 'idle'">
          <span class="pill-icon">{{ githubStatusIcon() }}</span>
          GitHub
          <span *ngIf="tokens.githubStatus() === 'ok'" class="pill-msg">{{ tokens.githubMessage() }}</span>
          <span *ngIf="tokens.githubStatus() === 'fail'" class="pill-msg-err">{{ tokens.githubMessage() }}</span>
          <span *ngIf="!tokens.hasGithub()" class="pill-msg-muted">not configured</span>
        </div>
        <button *ngIf="tokens.hasJira() || tokens.hasGithub()"
                class="btn-clear-tokens" (click)="clearAllTokens()">
          🗑 Clear All Tokens
        </button>
      </div>

      <!-- Expanded token form -->
      <div *ngIf="showTokens()" class="token-form-panel mt-16">

        <!-- JIRA Token -->
        <div class="token-section">
          <div class="token-section-header">
            <span class="token-section-icon">🔵</span>
            <span class="token-section-label">JIRA Personal Access Token</span>
            <a href="https://id.atlassian.com/manage-profile/security/api-tokens"
               target="_blank" rel="noopener" class="token-help-link">
              Create token ↗
            </a>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:10px">
            <div class="form-group" style="margin-bottom:0">
              <label class="form-label">JIRA Base URL</label>
              <input class="form-control" [(ngModel)]="jiraUrl"
                     placeholder="https://yourcompany.atlassian.net">
            </div>
            <div class="form-group" style="margin-bottom:0">
              <label class="form-label">API Token</label>
              <input class="form-control" [(ngModel)]="jiraPat"
                     type="password" placeholder="••••••••••••••••••">
            </div>
          </div>
          <div class="flex gap-8">
            <button class="btn btn-secondary btn-sm" (click)="saveJiraToken()"
                    [disabled]="!jiraUrl || !jiraPat">
              💾 Save Token
            </button>
            <button class="btn btn-primary btn-sm" (click)="testJira()"
                    [disabled]="!tokens.hasJira() || tokens.jiraStatus() === 'testing'">
              <span *ngIf="tokens.jiraStatus() === 'testing'"
                    class="spinner" style="width:12px;height:12px"></span>
              🔌 Test Connection
            </button>
          </div>
        </div>

        <div class="token-divider"></div>

        <!-- GitHub Token -->
        <div class="token-section">
          <div class="token-section-header">
            <span class="token-section-icon">🐙</span>
            <span class="token-section-label">GitHub Personal Access Token</span>
            <a href="https://github.com/settings/tokens"
               target="_blank" rel="noopener" class="token-help-link">
              Create token ↗
            </a>
          </div>
          <div class="form-group" style="margin-bottom:10px;max-width:50%">
            <label class="form-label">GitHub Token (classic or fine-grained)</label>
            <input class="form-control" [(ngModel)]="githubPat"
                   type="password" placeholder="ghp_••••••••••••••••••••">
          </div>
          <div class="flex gap-8">
            <button class="btn btn-secondary btn-sm" (click)="saveGithubToken()"
                    [disabled]="!githubPat">
              💾 Save Token
            </button>
            <button class="btn btn-primary btn-sm" (click)="testGithub()"
                    [disabled]="!tokens.hasGithub() || tokens.githubStatus() === 'testing'">
              <span *ngIf="tokens.githubStatus() === 'testing'"
                    class="spinner" style="width:12px;height:12px"></span>
              🔌 Test Connection
            </button>
          </div>
        </div>

        <div class="token-session-note">
          <span>🔒</span>
          <span>Tokens are stored in <strong>sessionStorage</strong> — cleared automatically
          when this browser tab closes. They are not sent to or stored on any server.</span>
        </div>
      </div>
    </div>

    <!-- ── Sprint health stats ───────────────────────────────────────── -->
    <div *ngIf="loading()" style="text-align:center;padding:64px">
      <div class="spinner" style="width:40px;height:40px;margin:0 auto 16px"></div>
      <p class="text-secondary">Loading sprint health data...</p>
    </div>

    <div *ngIf="!loading() && report()">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value" [style.color]="getScoreColor(report()!.overallHealthScore)">
            {{ report()!.overallHealthScore }}
          </div>
          <div class="stat-label">Health Score</div>
          <div class="health-bar-wrap mt-8">
            <div class="health-bar"
                 [style.width.%]="report()!.overallHealthScore"
                 [style.background]="getScoreColor(report()!.overallHealthScore)"></div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ report()!.totalIssues }}</div>
          <div class="stat-label">Total Stories</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:var(--warning)">{{ report()!.issuesWithGaps }}</div>
          <div class="stat-label">Stories with Gaps</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:var(--danger)">{{ report()!.flaggedIssues }}</div>
          <div class="stat-label">🚨 Flagged</div>
        </div>
      </div>

      <!-- Gap report table -->
      <div class="card mt-16" *ngIf="report()!.issueReports.length">
        <div class="card-title mb-16">Story Gap Analysis</div>
        <table class="data-table">
          <thead>
            <tr>
              <th>ISSUE</th><th>SUMMARY</th><th>SCORE</th><th>GAPS</th><th>STATUS</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let r of report()!.issueReports"
                [class.row-flagged]="r.flagged" (click)="toggleExpand(r.issueKey)">
              <td><app-jira-key [issueKey]="r.issueKey" /></td>
              <td>{{ r.issueSummary }}</td>
              <td>
                <span class="score-badge"
                      [style.background]="r.qualityScore>=75?'#dcfce7':r.qualityScore>=50?'#fef3c7':'#fee2e2'"
                      [style.color]="r.qualityScore>=75?'#166534':r.qualityScore>=50?'#92400e':'#991b1b'">
                  {{ r.qualityScore }}
                </span>
              </td>
              <td class="gap-badges">
                <span *ngIf="r.criticalCount" class="badge badge-danger">{{ r.criticalCount }} CRIT</span>
                <span *ngIf="r.errorCount"    class="badge badge-warning">{{ r.errorCount }} ERR</span>
                <span *ngIf="r.warningCount"  class="badge badge-gray">{{ r.warningCount }} WARN</span>
              </td>
              <td><span *ngIf="r.flagged">🚨</span><span *ngIf="!r.flagged">✅</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
  `,
  styles: [`
    /* Token panel */
    .card-header-row { display:flex;align-items:flex-start;justify-content:space-between;gap:16px; }
    .card-subtitle { font-size:12px;color:var(--text-muted);margin-top:4px;max-width:560px;line-height:1.5; }

    .token-status-row { display:flex;align-items:center;gap:10px;flex-wrap:wrap; }
    .token-status-pill {
      display:inline-flex;align-items:center;gap:6px;
      padding:4px 12px;border-radius:9999px;font-size:12px;font-weight:600;
      border:1.5px solid var(--border);background:var(--surface);color:var(--text-secondary);
      transition:all .15s;
    }
    .pill-set     { border-color:#bae6fd;background:#e0f2fe;color:#0369a1; }
    .pill-ok      { border-color:#bbf7d0;background:#dcfce7;color:#15803d; }
    .pill-fail    { border-color:#fca5a5;background:#fee2e2;color:#b91c1c; }
    .pill-testing { border-color:#e9d5ff;background:#f3e8ff;color:#7c3aed; }
    .pill-icon    { font-size:14px; }
    .pill-msg     { font-size:11px;font-weight:400;color:#15803d; }
    .pill-msg-err { font-size:11px;font-weight:400;color:#b91c1c; }
    .pill-msg-muted{font-size:11px;font-weight:400;color:var(--text-muted); }

    .btn-clear-tokens {
      font-size:11px;color:var(--danger);background:none;border:none;cursor:pointer;
      padding:4px 8px;text-decoration:underline;
    }

    .token-form-panel {
      border:1px solid var(--border);border-radius:var(--radius-lg);
      padding:20px;background:var(--bg);animation:slideDown .15s ease-out;
    }
    @keyframes slideDown { from{opacity:0;transform:translateY(-6px)} to{opacity:1;transform:translateY(0)} }

    .token-section { padding:4px 0; }
    .token-section-header {
      display:flex;align-items:center;gap:8px;margin-bottom:12px;
    }
    .token-section-icon  { font-size:18px; }
    .token-section-label { font-size:14px;font-weight:700;color:var(--text-primary); }
    .token-help-link {
      font-size:11px;color:var(--primary);text-decoration:none;margin-left:auto;
    }
    .token-help-link:hover { text-decoration:underline; }
    .token-divider {
      height:1px;background:var(--border);margin:16px 0;
    }
    .token-session-note {
      display:flex;align-items:flex-start;gap:8px;margin-top:16px;
      padding:10px 14px;background:#fffbeb;border:1px solid #fde68a;
      border-radius:var(--radius);font-size:12px;color:#78350f;line-height:1.5;
    }

    /* Dashboard table */
    .score-badge { padding:2px 8px;border-radius:9999px;font-size:11px;font-weight:700; }
    .row-flagged { background:#fff5f5; }
    .gap-badges  { display:flex;gap:4px;flex-wrap:wrap; }
  `]
})
export class DashboardComponent implements OnInit {
  projectKey = 'COMMSSURV';

  // Token form state
  jiraPat    = '';
  jiraUrl    = '';
  githubPat  = '';
  showTokens = signal(false);

  // Dashboard state
  loading  = signal(true);
  scanning = signal(false);
  report   = signal<SprintHealthReport | null>(null);
  expanded = signal<Set<string>>(new Set());

  constructor(
    private jira: JiraService,
    public  auth: AuthService,
    public  tokens: TokenService
  ) {}

  ngOnInit(): void {
    // Pre-fill URL field from session if already saved
    this.jiraUrl = this.tokens.jiraUrl();
    this.jira.getActiveSprintIssues(this.projectKey).subscribe({
      next: r => {
        // Build a minimal report from the issue list for display
        const issues = r.data ?? [];
        this.report.set({
          projectKey: this.projectKey, sprintName: 'Active Sprint',
          overallHealthScore: issues.length ? Math.round(issues.reduce((a: number, i: any) => a + (i.qualityScore ?? 80), 0) / issues.length) : 100,
          totalIssues: issues.length, issuesWithGaps: issues.filter((i:any)=>i.qualityScore<75).length,
          flaggedIssues: issues.filter((i:any)=>i.flagged).length, issueReports: issues
        } as any);
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); }
    });
  }

  runScan(): void {
    this.scanning.set(true);
    this.jira.scanProject(this.projectKey).subscribe({
      next: r => { this.report.set(r.data); this.scanning.set(false); },
      error: () => this.scanning.set(false)
    });
  }

  saveJiraToken(): void {
    this.tokens.setJira(this.jiraPat, this.jiraUrl);
    this.jiraPat = '';
  }
  saveGithubToken(): void {
    this.tokens.setGithub(this.githubPat);
    this.githubPat = '';
  }
  testJira():   void { this.tokens.testJira(); }
  testGithub(): void { this.tokens.testGithub(); }
  clearAllTokens(): void { this.tokens.clearAll(); this.jiraUrl = ''; }

  toggleExpand(key: string): void {
    const s = new Set(this.expanded());
    s.has(key) ? s.delete(key) : s.add(key);
    this.expanded.set(s);
  }

  jiraStatusIcon():   string {
    const s = this.tokens.jiraStatus();
    return s==='ok'?'✅':s==='fail'?'❌':s==='testing'?'⏳':this.tokens.hasJira()?'🔵':'⚪';
  }
  githubStatusIcon(): string {
    const s = this.tokens.githubStatus();
    return s==='ok'?'✅':s==='fail'?'❌':s==='testing'?'⏳':this.tokens.hasGithub()?'🐙':'⚪';
  }
  getScoreColor(s: number): string {
    return s>=75?'var(--success)':s>=50?'var(--warning)':'var(--danger)';
  }
}
