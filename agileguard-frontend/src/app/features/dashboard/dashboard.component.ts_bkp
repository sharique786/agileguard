import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { JiraService } from '../../core/services/jira.service';
import { AuthService } from '../../core/services/auth.service';
import { SprintHealthReport, GapReport } from '../../core/models';

/**
 * Main dashboard showing sprint health, flagged stories, and gap summary.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div>
      <div class="page-header flex justify-between items-center">
        <div>
          <h1 class="page-title">Sprint Health Dashboard</h1>
          <p class="page-subtitle">Real-time SDLC quality overview • Project: {{ projectKey }}</p>
        </div>
        <button class="btn btn-primary" (click)="runScan()" [disabled]="scanning()">
          <span *ngIf="scanning()" class="spinner" style="width:16px;height:16px"></span>
          {{ scanning() ? 'Scanning...' : '🔍 Run Gap Scan' }}
        </button>
      </div>

      <!-- Loading state -->
      <div *ngIf="loading()" style="text-align:center;padding:64px">
        <div class="spinner" style="width:40px;height:40px;margin:0 auto 16px"></div>
        <p class="text-secondary">Loading sprint health data...</p>
      </div>

      <div *ngIf="!loading() && report()">
        <!-- Stats Grid -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-value" [style.color]="getScoreColor(report()!.overallHealthScore)">
              {{ report()!.overallHealthScore }}
            </div>
            <div class="stat-label">Health Score</div>
            <div class="health-bar-wrap mt-8">
              <div class="health-bar" [style.width.%]="report()!.overallHealthScore"
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
          <div class="stat-card">
            <div class="stat-value">{{ report()!.averageQualityScore | number:'1.0-0' }}</div>
            <div class="stat-label">Avg Quality Score</div>
          </div>
        </div>

        <!-- Flagged / Aging issues alert -->
        <div class="alert alert-danger mb-24" *ngIf="report()!.agingIssueKeys.length > 0">
          <strong>⏰ Aging Stories Detected:</strong>
          <span *ngFor="let key of report()!.agingIssueKeys" class="badge badge-danger" style="margin-left:8px">{{ key }}</span>
          <span> — These stories have been in the same status past the threshold. Review in your next standup.</span>
        </div>

        <div class="grid-2">
          <!-- Story Quality Table -->
          <div class="card">
            <div class="card-title flex justify-between">
              Story Quality Overview
              <a routerLink="/reports" class="btn btn-secondary btn-sm">Full Report →</a>
            </div>
            <table class="table">
              <thead>
                <tr>
                  <th>Issue</th><th>Summary</th><th>Score</th><th>Gaps</th><th>Status</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let r of report()!.issueReports" [class.flagged-row]="r.flagged">
                  <td><strong>{{ r.issueKey }}</strong></td>
                  <td style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ r.issueSummary }}</td>
                  <td>
                    <span class="badge" [ngClass]="getScoreBadge(r.qualityScore)">{{ r.qualityScore }}</span>
                  </td>
                  <td>
                    <span *ngIf="r.criticalCount" class="badge badge-danger">{{ r.criticalCount }} CRIT</span>
                    <span *ngIf="r.errorCount" class="badge badge-warning" style="margin-left:4px">{{ r.errorCount }} ERR</span>
                    <span *ngIf="r.warningCount" class="badge badge-gray" style="margin-left:4px">{{ r.warningCount }} WARN</span>
                  </td>
                  <td>
                    <span *ngIf="r.flagged" title="Flagged for attention">🚨</span>
                    <span *ngIf="!r.flagged">✅</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Gap findings panel -->
          <div>
            <div class="card mb-16">
              <div class="card-title">Recent Gap Findings</div>
              <ng-container *ngFor="let r of report()!.issueReports">
                <ng-container *ngFor="let f of r.findings.slice(0,2)">
                  <div class="gap-item" [ngClass]="f.severity">
                    <span class="gap-severity" [ngClass]="f.severity">{{ f.severity }}</span>
                    <div>
                      <div style="font-size:13px;font-weight:500">{{ r.issueKey }}: {{ f.message }}</div>
                      <div style="font-size:12px;color:var(--text-secondary);margin-top:4px">💡 {{ f.suggestedAction }}</div>
                    </div>
                  </div>
                </ng-container>
              </ng-container>
              <div *ngIf="!hasFindigns()" class="text-secondary text-sm" style="padding:16px;text-align:center">No active gap findings 🎉</div>
            </div>

            <div class="card">
              <div class="card-title">Quick Actions</div>
              <a routerLink="/stories" class="btn btn-primary w-full mb-8" style="justify-content:center">
                + Create New Story with AI Assist
              </a>
              <a routerLink="/leaderboard" class="btn btn-secondary w-full" style="justify-content:center">
                🏆 View Team Leaderboard
              </a>
            </div>
          </div>
        </div>
      </div>

      <div *ngIf="!loading() && !report()" style="text-align:center;padding:64px">
        <div style="font-size:48px;margin-bottom:16px">🔍</div>
        <h2 style="font-size:18px;font-weight:600;margin-bottom:8px">No scan data yet</h2>
        <p class="text-secondary mb-16">Click "Run Gap Scan" to analyse your active sprint</p>
        <button class="btn btn-primary" (click)="runScan()">Run First Scan</button>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  report = signal<SprintHealthReport | null>(null);
  loading = signal(true);
  scanning = signal(false);
  projectKey = 'COMMSSURV';

  constructor(private jira: JiraService, public auth: AuthService) {}

  ngOnInit(): void { this.runScan(); }

  runScan(): void {
    this.scanning.set(true);
    this.loading.set(!this.report());
    this.jira.scanProject(this.projectKey).subscribe({
      next: (res) => { this.report.set(res.data); this.loading.set(false); this.scanning.set(false); },
      error: () => { this.loading.set(false); this.scanning.set(false); }
    });
  }

  hasFindigns(): boolean {
    return !!this.report()?.issueReports.some(r => r.findings.length > 0);
  }

  getScoreColor(score: number): string {
    if (score >= 75) return 'var(--success)';
    if (score >= 50) return 'var(--warning)';
    return 'var(--danger)';
  }

  getScoreBadge(score: number): string {
    if (score >= 75) return 'badge-success';
    if (score >= 50) return 'badge-warning';
    return 'badge-danger';
  }
}
