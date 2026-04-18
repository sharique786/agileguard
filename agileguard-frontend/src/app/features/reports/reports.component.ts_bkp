import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GapTypeLabelPipe } from '../../core/pipes/gap-type-label.pipe';
import { JiraService } from '../../core/services/jira.service';
import { GitHubService } from '../../core/services/github.service';
import { ExportService } from '../../core/services/export.service';
import { SprintHealthReport, GapReport, WorkflowRun } from '../../core/models';

/**
 * Gap Reports page.
 *
 * Key fix: every template reference to the report signal uses the
 * "*ngIf="report() as r"" pattern so TypeScript knows 'r' is non-null.
 * No "!" non-null assertions are needed anywhere in the template.
 *
 * Features:
 *   • Sprint health summary with animated health bar
 *   • Severity filter tabs (All / Critical / Error / Warning / Clean)
 *   • Free-text search across issue key and summary
 *   • Flagged-only toggle
 *   • Issue cards with expandable per-finding rows
 *   • CI/CD workflow run table with its own CSV export
 *   • Export toolbar: CSV (all gaps), CSV (flagged), CSV (CI/CD),
 *                     JSON, HTML (standalone), PDF (print dialog)
 */
@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, GapTypeLabelPipe],
  template: `
    <div>

      <!-- ── Page Header ──────────────────────────────────────────────────── -->
      <div class="page-header flex justify-between items-center"
           style="flex-wrap:wrap;gap:12px">
        <div>
          <h1 class="page-title">📈 Gap &amp; Health Reports</h1>
          <!-- Use optional chaining outside the guarded block -->
          <p class="page-subtitle">
            Sprint quality analysis · Project: <strong>{{ projectKey }}</strong>
            <span *ngIf="report()?.generatedAt"
                  class="text-muted" style="margin-left:8px;font-size:12px">
              Last scan: {{ formatDate(report()?.generatedAt) }}
            </span>
          </p>
        </div>

        <div class="flex gap-8" style="flex-wrap:wrap">
          <!-- Refresh -->
          <button class="btn btn-secondary btn-sm"
                  (click)="loadAll()" [disabled]="loading()">
            <span *ngIf="loading()" class="spinner"
                  style="width:14px;height:14px"></span>
            {{ loading() ? 'Loading…' : '🔄 Refresh' }}
          </button>

          <!-- Export dropdown (disabled until data is loaded) -->
          <div style="position:relative">
            <button class="btn btn-primary btn-sm"
                    (click)="toggleExportMenu()"
                    [disabled]="!report()"
                    style="gap:6px">
              ⬇ Export <span style="font-size:10px">▾</span>
            </button>

            <div *ngIf="exportMenuOpen()" class="export-menu">
              <div class="export-menu-header">Download As</div>

              <button class="export-item" (click)="exportCsv()">
                <span class="export-icon">📊</span>
                <div>
                  <div class="export-label">CSV — All Gaps</div>
                  <div class="export-sub">Every finding, one row per gap</div>
                </div>
              </button>

              <button class="export-item" (click)="exportFlaggedCsv()">
                <span class="export-icon">🚨</span>
                <div>
                  <div class="export-label">CSV — Flagged Only</div>
                  <div class="export-sub">Critical &amp; high-risk stories</div>
                </div>
              </button>

              <button class="export-item"
                      (click)="exportCiCsv()"
                      [disabled]="!workflowRuns().length">
                <span class="export-icon">⚡</span>
                <div>
                  <div class="export-label">CSV — CI/CD Runs</div>
                  <div class="export-sub">GitHub Actions pipeline data</div>
                </div>
              </button>

              <div class="export-divider"></div>

              <button class="export-item" (click)="exportJson()">
                <span class="export-icon">{{ '{' }} {{ '}' }}</span>
                <div>
                  <div class="export-label">JSON — Full Report</div>
                  <div class="export-sub">Complete structured data dump</div>
                </div>
              </button>

              <button class="export-item" (click)="exportHtml()">
                <span class="export-icon">🌐</span>
                <div>
                  <div class="export-label">HTML — Shareable Report</div>
                  <div class="export-sub">Self-contained, email-ready</div>
                </div>
              </button>

              <button class="export-item" (click)="exportPdf()">
                <span class="export-icon">📄</span>
                <div>
                  <div class="export-label">PDF — Print / Save</div>
                  <div class="export-sub">Opens print dialog → Save as PDF</div>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Loading state ────────────────────────────────────────────────── -->
      <div *ngIf="loading()" style="text-align:center;padding:64px">
        <div class="spinner" style="width:40px;height:40px;margin:0 auto 16px"></div>
        <p class="text-secondary">Loading sprint health data…</p>
      </div>

      <!--
        ══════════════════════════════════════════════════════════════════════
        KEY FIX: "*ngIf="report() as r"" narrows SprintHealthReport|null
        to SprintHealthReport so every child reference to "r" is null-safe.
        No "!" assertions are needed anywhere below this line.
        ══════════════════════════════════════════════════════════════════════
      -->
      <ng-container *ngIf="!loading() && report() as r">

        <!-- ── Summary stats ─────────────────────────────────────────────── -->
        <div class="stats-grid mb-24">

          <div class="stat-card">
            <div class="stat-value"
                 [style.color]="scoreColor(r.overallHealthScore)">
              {{ r.overallHealthScore }}<span style="font-size:18px">%</span>
            </div>
            <div class="stat-label">Health Score</div>
            <div class="health-bar-wrap mt-8">
              <div class="health-bar"
                   [style.width.%]="r.overallHealthScore"
                   [style.background]="scoreColor(r.overallHealthScore)">
              </div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-value">{{ r.totalIssues }}</div>
            <div class="stat-label">Total Stories</div>
          </div>

          <div class="stat-card">
            <div class="stat-value" style="color:var(--warning)">
              {{ r.issuesWithGaps }}
            </div>
            <div class="stat-label">Have Gaps</div>
          </div>

          <div class="stat-card">
            <div class="stat-value" style="color:var(--danger)">
              {{ r.flaggedIssues }}
            </div>
            <div class="stat-label">🚨 Flagged</div>
          </div>

          <div class="stat-card">
            <div class="stat-value"
                 [style.color]="scoreColor(r.averageQualityScore)">
              {{ r.averageQualityScore | number:'1.0-0' }}
            </div>
            <div class="stat-label">Avg Quality Score</div>
          </div>

          <div class="stat-card">
            <div class="stat-value" style="color:var(--danger)">
              {{ criticalTotal() }}
            </div>
            <div class="stat-label">Total CRITICAL Gaps</div>
          </div>

        </div>

        <!-- ── Aging alert ────────────────────────────────────────────────── -->
        <!--
          r.agingIssueKeys is now typed as string[] (non-null) so the
          comparison is straightforward — no optional chaining needed.
        -->
        <div class="alert alert-danger mb-24"
             *ngIf="r.agingIssueKeys && r.agingIssueKeys.length > 0">
          <strong>⏰ Aging Stories Detected:</strong>
          <span *ngFor="let key of r.agingIssueKeys"
                class="badge badge-danger" style="margin-left:8px">
            {{ key }}
          </span>
          <span class="text-sm" style="margin-left:8px">
            — In the same status past the configured threshold.
            Raise in your next standup.
          </span>
        </div>

        <!-- ── Filters & search bar ───────────────────────────────────────── -->
        <div class="card mb-16" style="padding:16px 20px">
          <div class="flex items-center gap-16" style="flex-wrap:wrap">

            <!-- Severity tabs -->
            <div class="flex gap-4">
              <button *ngFor="let tab of severityTabs"
                      class="btn btn-sm"
                      [ngClass]="severityFilter() === tab.value
                                  ? 'btn-primary' : 'btn-secondary'"
                      (click)="severityFilter.set(tab.value)">
                {{ tab.label }}
                <span class="badge badge-gray"
                      style="margin-left:4px;font-size:10px">
                  {{ tab.count() }}
                </span>
              </button>
            </div>

            <!-- Search input -->
            <div style="flex:1;min-width:200px;position:relative">
              <span style="position:absolute;left:10px;top:50%;
                           transform:translateY(-50%);color:var(--text-muted)">
                🔍
              </span>
              <input type="text" class="form-control"
                     style="padding-left:32px"
                     placeholder="Search by issue key or summary…"
                     [ngModel]="searchQuery()"
                     (ngModelChange)="searchQuery.set($event)">
            </div>

            <!-- Flagged-only toggle -->
            <label class="flex items-center gap-8"
                   style="cursor:pointer;font-size:13px;font-weight:500;
                          white-space:nowrap">
              <span style="position:relative;display:inline-block;
                           width:40px;height:22px">
                <input type="checkbox"
                       style="opacity:0;width:0;height:0"
                       [checked]="flaggedOnly()"
                       (change)="flaggedOnly.set(!flaggedOnly())">
                <span style="position:absolute;inset:0;border-radius:11px;
                             cursor:pointer;transition:.2s"
                      [style.background]="flaggedOnly()
                                          ? 'var(--danger)' : 'var(--border)'">
                  <span style="position:absolute;left:3px;top:3px;
                               width:16px;height:16px;border-radius:50%;
                               background:#fff;transition:.2s"
                        [style.transform]="flaggedOnly()
                                           ? 'translateX(18px)' : 'translateX(0)'">
                  </span>
                </span>
              </span>
              🚨 Flagged only
            </label>

            <!-- Result count -->
            <span class="text-secondary text-sm">
              Showing {{ filtered().length }} of
              {{ r.issueReports.length }} stories
            </span>

            <!-- Clear filters -->
            <button class="btn btn-secondary btn-sm"
                    *ngIf="hasActiveFilters()"
                    (click)="resetFilters()">
              ✕ Clear filters
            </button>

          </div>
        </div>

        <!-- ── Empty filtered state ───────────────────────────────────────── -->
        <div *ngIf="filtered().length === 0" class="card"
             style="text-align:center;padding:48px;color:var(--text-secondary)">
          <div style="font-size:36px;margin-bottom:12px">🎉</div>
          <div style="font-weight:600;margin-bottom:6px">
            No stories match current filters
          </div>
          <div class="text-sm">
            Try adjusting the severity filter or search term.
          </div>
        </div>

        <!-- ── Issue gap cards ────────────────────────────────────────────── -->
        <div *ngFor="let issue of filtered()"
             class="issue-card mb-16"
             [class.issue-card-flagged]="issue.flagged">

          <!-- Card header (clickable to expand) -->
          <div class="issue-header" (click)="toggleExpand(issue.issueKey)">
            <div class="flex items-center gap-12"
                 style="flex:1;min-width:0">
              <span class="issue-key">{{ issue.issueKey }}</span>
              <span class="issue-summary">{{ issue.issueSummary }}</span>
              <span *ngIf="issue.flagged"
                    class="badge badge-danger">🚨 Flagged</span>
            </div>
            <div class="flex items-center gap-12" style="flex-shrink:0">
              <div class="flex gap-4">
                <span *ngIf="issue.criticalCount"
                      class="badge badge-danger">
                  {{ issue.criticalCount }} CRITICAL
                </span>
                <span *ngIf="issue.errorCount"
                      class="badge badge-warning">
                  {{ issue.errorCount }} ERROR
                </span>
                <span *ngIf="issue.warningCount"
                      class="badge badge-gray">
                  {{ issue.warningCount }} WARNING
                </span>
                <span *ngIf="!issue.findings.length"
                      class="badge badge-success">✅ Clean</span>
              </div>
              <div class="score-ring"
                   style="width:44px;height:44px;font-size:14px"
                   [ngClass]="scoreRingClass(issue.qualityScore)">
                {{ issue.qualityScore }}
              </div>
              <span style="color:var(--text-muted);font-size:18px">
                {{ isExpanded(issue.issueKey) ? '▲' : '▼' }}
              </span>
            </div>
          </div>

          <!-- Expanded findings list -->
          <div *ngIf="isExpanded(issue.issueKey)" class="findings-body">

            <div *ngIf="issue.findings.length === 0"
                 style="padding:16px;color:var(--success);font-size:13px">
              ✅ No gaps detected — this story meets all quality rules.
            </div>

            <div *ngFor="let f of issue.findings"
                 class="finding-row"
                 [ngClass]="'finding-' + f.severity.toLowerCase()">
              <span class="finding-sev"
                    [ngClass]="'sev-' + f.severity.toLowerCase()">
                {{ f.severity }}
              </span>
              <div class="finding-type">{{ f.type | gapTypeLabel }}</div>
              <div class="finding-content">
                <div class="finding-message">{{ f.message }}</div>
                <div class="finding-action">💡 {{ f.suggestedAction }}</div>
              </div>
              <div class="finding-date text-muted text-xs"
                   *ngIf="f.detectedAt">
                {{ formatDate(f.detectedAt) }}
              </div>
            </div>

          </div>
        </div>

        <!-- ── Export hint ────────────────────────────────────────────────── -->
        <div class="alert alert-info mb-24" style="font-size:13px">
          <strong>📤 Export this report:</strong>
          Use the <strong>⬇ Export</strong> button above to download as
          <span class="badge badge-info"
                style="cursor:pointer" (click)="exportCsv()">CSV</span>
          <span class="badge badge-info"
                style="cursor:pointer;margin-left:4px"
                (click)="exportJson()">JSON</span>
          <span class="badge badge-info"
                style="cursor:pointer;margin-left:4px"
                (click)="exportHtml()">HTML</span>
          or
          <span class="badge badge-info"
                style="cursor:pointer;margin-left:4px"
                (click)="exportPdf()">PDF</span>
          — shareable with your team or stakeholders.
        </div>

        <!-- ── CI/CD runs table ───────────────────────────────────────────── -->
        <div class="card" *ngIf="workflowRuns().length > 0">
          <div class="card-title flex justify-between items-center">
            <span>⚡ GitHub Actions — Recent Runs</span>
            <button class="btn btn-secondary btn-sm" (click)="exportCiCsv()">
              ⬇ Export CSV
            </button>
          </div>
          <table class="table">
            <thead>
              <tr>
                <th>Workflow</th>
                <th>Branch</th>
                <th>Conclusion</th>
                <th>Triggered By</th>
                <th>JIRA Issue</th>
                <th>Duration</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let run of workflowRuns()">
                <td style="font-weight:500">{{ run.workflowName }}</td>
                <td style="font-family:monospace;font-size:12px">
                  {{ run.headBranch }}
                </td>
                <td>
                  <span class="badge"
                        [ngClass]="run.conclusion === 'success' ? 'badge-success'
                                 : run.conclusion === 'failure' ? 'badge-danger'
                                 : 'badge-gray'">
                    {{ run.conclusion || run.status }}
                  </span>
                </td>
                <td class="text-sm text-secondary">
                  {{ run.triggeredBy || '—' }}
                </td>
                <td>
                  <span *ngIf="run.jiraIssueKey"
                        class="badge badge-purple">
                    {{ run.jiraIssueKey }}
                  </span>
                  <span *ngIf="!run.jiraIssueKey"
                        class="text-muted text-xs">—</span>
                </td>
                <td class="text-sm text-secondary">
                  {{ run.durationSeconds }}s
                </td>
              </tr>
            </tbody>
          </table>
        </div>

      </ng-container><!-- end *ngIf="report() as r" -->

      <!-- ── Empty state (no data yet) ─────────────────────────────────────── -->
      <div *ngIf="!loading() && !report()"
           style="text-align:center;padding:64px">
        <div style="font-size:48px;margin-bottom:16px">📊</div>
        <h2 style="font-size:18px;font-weight:600;margin-bottom:8px">
          No report data yet
        </h2>
        <p class="text-secondary mb-16">
          Click Refresh to load the latest sprint gap data.
        </p>
        <button class="btn btn-primary" (click)="loadAll()">Load Report</button>
      </div>

    </div>
  `,
  styles: [`
    /* ── Export dropdown ──────────────────────────────────────────────────── */
    .export-menu {
      position: absolute;
      top: calc(100% + 6px);
      right: 0;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      box-shadow: 0 10px 30px rgba(0,0,0,0.12);
      width: 280px;
      z-index: 1000;
      overflow: hidden;
    }
    .export-menu-header {
      padding: 10px 16px 8px;
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.07em;
      color: var(--text-muted);
      border-bottom: 1px solid var(--border);
    }
    .export-item {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;
      padding: 11px 16px;
      border: none;
      background: transparent;
      cursor: pointer;
      text-align: left;
      transition: background 0.12s;
    }
    .export-item:hover   { background: var(--bg); }
    .export-item:disabled { opacity: 0.4; cursor: not-allowed; }
    .export-icon  { font-size: 20px; width: 28px; text-align: center; flex-shrink: 0; }
    .export-label { font-size: 13px; font-weight: 600; color: var(--text-primary); }
    .export-sub   { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
    .export-divider { height: 1px; background: var(--border); margin: 4px 0; }

    /* ── Issue cards ──────────────────────────────────────────────────────── */
    .issue-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    }
    .issue-card-flagged { border-color: #fca5a5; background: #fff5f5; }

    .issue-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 20px;
      cursor: pointer;
      transition: background 0.12s;
      user-select: none;
    }
    .issue-header:hover { background: var(--bg); }

    .issue-key {
      font-weight: 700;
      font-size: 14px;
      color: var(--text-primary);
      white-space: nowrap;
    }
    .issue-summary {
      font-size: 13px;
      color: var(--text-secondary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
      min-width: 0;
    }

    /* ── Finding rows ─────────────────────────────────────────────────────── */
    .findings-body   { border-top: 1px solid var(--border); }
    .finding-row {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 20px;
      border-bottom: 1px solid var(--border);
      font-size: 13px;
    }
    .finding-row:last-child { border-bottom: none; }
    .finding-critical { background: #fff5f5; }
    .finding-error    { background: #fff7ed; }
    .finding-warning  { background: #fffbeb; }

    .finding-sev {
      font-size: 10px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
      white-space: nowrap;
      flex-shrink: 0;
      margin-top: 2px;
    }
    .sev-critical { background: #fee2e2; color: #991b1b; }
    .sev-error    { background: #ffedd5; color: #9a3412; }
    .sev-warning  { background: #fef3c7; color: #92400e; }
    .sev-info     { background: #e0f2fe; color: #0c4a6e; }

    .finding-type {
      font-weight: 600;
      color: var(--text-primary);
      white-space: nowrap;
      width: 200px;
      flex-shrink: 0;
      padding-top: 2px;
    }
    .finding-content  { flex: 1; min-width: 0; }
    .finding-message  { color: var(--text-primary); margin-bottom: 4px; }
    .finding-action   { font-size: 12px; color: var(--text-secondary); }
    .finding-date     {
      flex-shrink: 0;
      padding-top: 2px;
      min-width: 120px;
      text-align: right;
    }
  `]
})
export class ReportsComponent implements OnInit {

  // ── Signals ──────────────────────────────────────────────────────────────
  report         = signal<SprintHealthReport | null>(null);
  workflowRuns   = signal<WorkflowRun[]>([]);
  loading        = signal(true);
  exportMenuOpen = signal(false);

  // ── Filter state ─────────────────────────────────────────────────────────
  severityFilter = signal<string>('ALL');
  searchQuery    = signal('');
  flaggedOnly    = signal(false);
  expandedKeys   = signal<Set<string>>(new Set());

  projectKey = 'COMMSSURV';

  // ── Severity tab definitions with live computed counts ───────────────────
  severityTabs = [
    {
      label: 'All', value: 'ALL',
      count: computed(() => this.report()?.issueReports.length ?? 0)
    },
    {
      label: '🚨 Critical', value: 'CRITICAL',
      count: computed(() =>
        this.report()?.issueReports.filter(r => r.criticalCount > 0).length ?? 0)
    },
    {
      label: '❌ Error', value: 'ERROR',
      count: computed(() =>
        this.report()?.issueReports.filter(r => r.errorCount > 0).length ?? 0)
    },
    {
      label: '⚠ Warning', value: 'WARNING',
      count: computed(() =>
        this.report()?.issueReports
          .filter(r => r.warningCount > 0 && !r.criticalCount && !r.errorCount)
          .length ?? 0)
    },
    {
      label: '✅ Clean', value: 'CLEAN',
      count: computed(() =>
        this.report()?.issueReports.filter(r => r.findings.length === 0).length ?? 0)
    },
  ];

  // ── Derived / computed values ─────────────────────────────────────────────

  /** Issue list after applying all active filters. */
  filtered = computed<GapReport[]>(() => {
    const all  = this.report()?.issueReports ?? [];
    const sev  = this.severityFilter();
    const q    = this.searchQuery().toLowerCase().trim();
    const flag = this.flaggedOnly();

    return all.filter(r => {
      if (sev === 'CRITICAL' && !r.criticalCount)  return false;
      if (sev === 'ERROR'    && !r.errorCount)      return false;
      if (sev === 'WARNING'  && (!r.warningCount || r.criticalCount || r.errorCount)) return false;
      if (sev === 'CLEAN'    && r.findings.length)  return false;
      if (flag && !r.flagged)                        return false;
      if (q && !r.issueKey.toLowerCase().includes(q)
            && !r.issueSummary.toLowerCase().includes(q)) return false;
      return true;
    });
  });

  /** Sum of all CRITICAL findings across the report. */
  criticalTotal = computed(() =>
    (this.report()?.issueReports ?? []).reduce((s, r) => s + r.criticalCount, 0)
  );

  /** True when any filter is non-default. */
  hasActiveFilters = computed(() =>
    this.severityFilter() !== 'ALL' || !!this.searchQuery() || this.flaggedOnly()
  );

  constructor(
    private jira: JiraService,
    private github: GitHubService,
    private exportSvc: ExportService
  ) {}

  ngOnInit(): void { this.loadAll(); }

  // ── Data loading ──────────────────────────────────────────────────────────

  loadAll(): void {
    this.loading.set(true);

    this.jira.scanProject(this.projectKey).subscribe({
      next: res => {
        this.report.set(res.data);
        this.loading.set(false);
        // Auto-expand flagged stories on first load
        const flaggedKeys = new Set(
          res.data.issueReports.filter(r => r.flagged).map(r => r.issueKey)
        );
        this.expandedKeys.set(flaggedKeys);
      },
      error: () => this.loading.set(false)
    });

    this.github.getWorkflowRuns('db-platform', 'payments-service').subscribe({
      next: res => this.workflowRuns.set(res.data),
      error: () => {}
    });
  }

  // ── Expand / collapse ─────────────────────────────────────────────────────

  toggleExpand(key: string): void {
    const s = new Set(this.expandedKeys());
    s.has(key) ? s.delete(key) : s.add(key);
    this.expandedKeys.set(s);
  }

  isExpanded(key: string): boolean { return this.expandedKeys().has(key); }

  // ── Filter helpers ────────────────────────────────────────────────────────

  resetFilters(): void {
    this.severityFilter.set('ALL');
    this.searchQuery.set('');
    this.flaggedOnly.set(false);
  }

  toggleExportMenu(): void { this.exportMenuOpen.set(!this.exportMenuOpen()); }

  // ── Export actions ────────────────────────────────────────────────────────

  exportCsv(): void {
    const r = this.report(); if (!r) return;
    this.exportSvc.exportGapsAsCsv(r);
    this.exportMenuOpen.set(false);
  }

  exportFlaggedCsv(): void {
    const r = this.report(); if (!r) return;
    this.exportSvc.exportFlaggedAsCsv(r);
    this.exportMenuOpen.set(false);
  }

  exportCiCsv(): void {
    this.exportSvc.exportWorkflowRunsAsCsv(this.workflowRuns(), this.projectKey);
    this.exportMenuOpen.set(false);
  }

  exportJson(): void {
    const r = this.report(); if (!r) return;
    this.exportSvc.exportAsJson(r);
    this.exportMenuOpen.set(false);
  }

  exportHtml(): void {
    const r = this.report(); if (!r) return;
    this.exportSvc.exportAsHtml(r, this.workflowRuns());
    this.exportMenuOpen.set(false);
  }

  exportPdf(): void {
    const r = this.report(); if (!r) return;
    this.exportSvc.exportAsPdf(r, this.workflowRuns());
    this.exportMenuOpen.set(false);
  }

  // ── Display helpers ───────────────────────────────────────────────────────

  scoreColor(score: number): string {
    return score >= 75 ? 'var(--success)'
         : score >= 50 ? 'var(--warning)'
         :               'var(--danger)';
  }

  scoreRingClass(score: number): string {
    return score >= 75 ? 'high' : score >= 50 ? 'mid' : 'low';
  }

  formatDate(iso: string | undefined): string {
    if (!iso) return '';
    try { return new Date(iso).toLocaleString(); } catch { return iso; }
  }
}