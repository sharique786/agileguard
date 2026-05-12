import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { JiraService } from '../../core/services/jira.service';
import { ExportService } from '../../core/services/export.service';
import { PiSprintReport, TeamSprintReport, SprintStoryDetail, JiraSprint } from '../../core/models';

const DEFAULT_PROJECT = 'COMMSSURV';

/**
 * SAFe Sprint Report Dashboard.
 *
 * Displays per-feature-team sprint metrics in line with the SAFe framework:
 *   • Predictability    (Accepted SP / Committed SP × 100)
 *   • Velocity          (Accepted story points)
 *   • Story Points Committed / Completed / Spilled
 *   • Stories Planned / Accepted
 *   • Capacity Planned / Actual / Utilisation %
 *   • Stories Completed table (all accepted stories)
 *   • Stories Spilled / Moved Out table (not completed)
 *   • Added mid-sprint stories (scope creep tracking)
 *
 * Supports:
 *   - PI + Sprint selectors
 *   - Team filter (show one team or all)
 *   - Expand / collapse per-team story tables
 *   - Export as CSV or PDF
 */
@Component({
  selector: 'app-sprint-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div>

    <!-- ── Page Header ────────────────────────────────────────────────────── -->
    <div class="page-header flex justify-between items-center" style="flex-wrap:wrap;gap:12px">
      <div>
        <h1 class="page-title">📋 SAFe Sprint Report</h1>
        <p class="page-subtitle">
          Feature team predictability, velocity and capacity tracking across
          <strong>{{ selectedPi || 'current PI' }}</strong>
        </p>
      </div>
      <div class="flex gap-8" style="flex-wrap:wrap">
        <button class="btn btn-secondary btn-sm" (click)="load()" [disabled]="loading()">
          🔄 {{ loading() ? 'Loading…' : 'Refresh' }}
        </button>
        <div style="position:relative">
          <button class="btn btn-primary btn-sm"
                  [disabled]="!report()"
                  (click)="toggleExport()">
            ⬇ Export ▾
          </button>
          <div *ngIf="exportOpen()" class="export-menu">
            <div class="export-menu-header">Download As</div>
            <button class="export-item" (click)="exportCsv()">
              <span class="export-icon">📊</span>
              <div>
                <div class="export-label">CSV — Full Sprint Report</div>
                <div class="export-sub">All teams, all metrics, all stories</div>
              </div>
            </button>
            <button class="export-item" (click)="exportPdf()">
              <span class="export-icon">📄</span>
              <div>
                <div class="export-label">PDF — Print / Save</div>
                <div class="export-sub">Opens print dialog → Save as PDF</div>
              </div>
            </button>
            <button class="export-item" (click)="exportHtml()">
              <span class="export-icon">🌐</span>
              <div>
                <div class="export-label">HTML — Shareable Page</div>
                <div class="export-sub">Self-contained, email-ready</div>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Selector bar ──────────────────────────────────────────────────── -->
    <div class="card mb-16" style="padding:14px 20px">
      <div class="flex gap-16 items-center" style="flex-wrap:wrap">

        <!-- PI selector -->
        <div class="flex items-center gap-8">
          <label class="form-label" style="margin:0;white-space:nowrap;font-size:13px">
            Program Increment
          </label>
          <select class="form-control" style="width:120px"
                  [(ngModel)]="selectedPi" (ngModelChange)="load()">
            <option *ngFor="let pi of piOptions" [value]="pi">{{ pi }}</option>
          </select>
        </div>

        <!-- Sprint selector -->
        <div class="flex items-center gap-8">
          <label class="form-label" style="margin:0;white-space:nowrap;font-size:13px">Sprint</label>
          <select class="form-control" style="width:280px"
                  [(ngModel)]="selectedSprintId"
                  (ngModelChange)="onSprintChange($event)">
            <option [ngValue]="null">— Active Sprint —</option>
            <option *ngFor="let s of sprints()" [ngValue]="s.id">
              {{ s.name }}
              <span *ngIf="s.state === 'active'"> (Active)</span>
              <span *ngIf="s.state === 'closed'"> (Closed)</span>
              <span *ngIf="s.state === 'future'"> (Future)</span>
            </option>
          </select>
        </div>

        <!-- Team filter -->
        <div class="flex items-center gap-8">
          <label class="form-label" style="margin:0;white-space:nowrap;font-size:13px">
            Feature Team
          </label>
          <select class="form-control" style="width:200px" [(ngModel)]="teamFilter">
            <option value="">All Teams</option>
            <option *ngFor="let t of report()?.teamReports" [value]="t.featureTeamId">
              {{ t.featureTeamName }}
            </option>
          </select>
        </div>

        <!-- Sprint info pill -->
        <div *ngIf="report() as r" class="flex items-center gap-8" style="margin-left:auto">
          <span class="badge" [ngClass]="sprintStateBadge(r.sprintState)">
            {{ r.sprintState | uppercase }}
          </span>
          <span *ngIf="r.sprintStartDate" class="text-muted text-sm">
            {{ r.sprintStartDate | date:'d MMM' }} –
            {{ r.sprintEndDate | date:'d MMM yyyy' }}
          </span>
        </div>
      </div>
    </div>

    <!-- ── Loading ─────────────────────────────────────────────────────────── -->
    <div *ngIf="loading()" style="text-align:center;padding:64px">
      <div class="spinner" style="width:42px;height:42px;margin:0 auto 16px"></div>
      <p class="text-secondary">Fetching JIRA sprint data…</p>
    </div>

    <ng-container *ngIf="!loading() && report() as r">

      <!-- ── Programme Summary ────────────────────────────────────────────── -->
      <div class="card mb-24">
        <div class="card-title flex justify-between items-center">
          <span>🏛️ Programme Summary — {{ r.sprintName }}</span>
          <span *ngIf="r.sprintGoal" class="text-sm text-secondary" style="font-weight:400;max-width:500px">
            Goal: {{ r.sprintGoal }}
          </span>
        </div>

        <div class="stats-grid">
          <div class="stat-card" [class.stat-card-success]="programmePredGood(r)"
               [class.stat-card-danger]="!programmePredGood(r)">
            <div class="stat-value"
                 [style.color]="predColor(r.programmePredictability)">
              {{ r.programmePredictability != null ? (r.programmePredictability | number:'1.0-1') + '%' : '—' }}
            </div>
            <div class="stat-label">Programme Predictability</div>
            <div class="stat-target">
              Target ≥ 80% &nbsp;
              <span *ngIf="programmePredGood(r)">✅</span>
              <span *ngIf="!programmePredGood(r)">⚠️</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-value" style="color:var(--primary)">{{ r.totalVelocity }}</div>
            <div class="stat-label">Total Velocity (SP)</div>
            <div class="stat-target">{{ r.totalPointsCommitted }} committed</div>
          </div>

          <div class="stat-card">
            <div class="stat-value">{{ r.totalStoriesAccepted }}</div>
            <div class="stat-label">Stories Accepted</div>
            <div class="stat-target">of {{ r.totalStoriesPlanned }} planned</div>
          </div>

          <div class="stat-card" [class.stat-card-warning]="r.totalStoriesSpilled > 0">
            <div class="stat-value"
                 [style.color]="r.totalStoriesSpilled > 0 ? 'var(--warning)' : 'var(--success)'">
              {{ r.totalStoriesSpilled }}
            </div>
            <div class="stat-label">Stories Spilled</div>
            <div class="stat-target">
              {{ r.totalStoriesSpilled > 0 ? 'Moved out / not completed' : 'None — clean sprint 🎉' }}
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-value">{{ r.totalCapacityPlanned | number:'1.0-0' }}h</div>
            <div class="stat-label">Capacity Planned</div>
            <div class="stat-target">{{ r.totalCapacityActual | number:'1.0-0' }}h actual</div>
          </div>

          <div class="stat-card">
            <div class="stat-value">{{ teamCount() }}</div>
            <div class="stat-label">Feature Teams</div>
            <div class="stat-target">{{ teamsAtTarget() }} met predictability target</div>
          </div>
        </div>
      </div>

      <!-- ── Predictability chart bar ─────────────────────────────────────── -->
      <div class="card mb-24">
        <div class="card-title">📊 Team Predictability vs 80% Target</div>
        <div style="padding:8px 0">
          <div *ngFor="let t of visibleTeams()" class="pred-bar-row">
            <div class="pred-bar-label">{{ t.featureTeamName }}</div>
            <div class="pred-bar-track">
              <div class="pred-bar-fill"
                   [style.width.%]="Math.min(t.predictability, 100)"
                   [style.background]="predColor(t.predictability)">
              </div>
              <!-- 80% target line -->
              <div class="pred-target-line"></div>
            </div>
            <div class="pred-bar-value"
                 [style.color]="predColor(t.predictability)">
              {{ t.predictability | number:'1.0-0' }}%
              <span class="text-xs text-muted">({{ t.storyPointsAccepted }}/{{ t.storyPointsCommitted }} SP)</span>
            </div>
            <span class="badge" [ngClass]="t.meetsPredictabilityTarget ? 'badge-success' : 'badge-warning'"
                  style="margin-left:8px">
              {{ t.meetsPredictabilityTarget ? '✅ On Target' : '⚠ Below Target' }}
            </span>
          </div>
        </div>
      </div>

      <!-- ── Per-team detail cards ────────────────────────────────────────── -->
      <div *ngFor="let team of visibleTeams()" class="team-card mb-24">

        <!-- Team card header -->
        <div class="team-card-header"
             [class.team-header-success]="team.meetsPredictabilityTarget"
             [class.team-header-warning]="!team.meetsPredictabilityTarget">
          <div class="flex items-center gap-12" style="flex:1;min-width:0">
            <div class="team-avatar">{{ teamInitials(team.featureTeamName) }}</div>
            <div>
              <div class="team-name">{{ team.featureTeamName }}</div>
              <div class="team-meta">
                {{ team.jiraComponent }} component &nbsp;·&nbsp;
                {{ team.teamSize }} members &nbsp;·&nbsp;
                <span class="badge badge-gray" style="font-size:10px">{{ team.sprintState }}</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-16">
            <!-- Velocity trend -->
            <div class="trend-badge" [ngClass]="trendClass(team.velocityTrend)">
              {{ trendIcon(team.velocityTrend) }} {{ team.velocityTrend }}
            </div>
            <!-- Summary toggle -->
            <button class="btn btn-secondary btn-sm" (click)="toggleTeam(team.featureTeamId)">
              {{ expandedTeams().has(team.featureTeamId) ? '▲ Collapse' : '▼ Details' }}
            </button>
          </div>
        </div>

        <!-- Summary narrative -->
        <div class="team-summary" *ngIf="team.summary">
          💬 {{ team.summary }}
        </div>

        <!-- Metrics row -->
        <div class="team-metrics-grid">

          <div class="metric-cell"
               [class.metric-cell-good]="team.meetsPredictabilityTarget"
               [class.metric-cell-bad]="!team.meetsPredictabilityTarget">
            <div class="metric-value" [style.color]="predColor(team.predictability)">
              {{ team.predictability | number:'1.0-0' }}%
            </div>
            <div class="metric-label">Predictability</div>
            <div class="metric-sub">Target ≥ 80%</div>
          </div>

          <div class="metric-cell">
            <div class="metric-value" style="color:var(--primary)">{{ team.velocity }}</div>
            <div class="metric-label">Velocity (SP)</div>
            <div class="metric-sub">Accepted points</div>
          </div>

          <div class="metric-cell">
            <div class="metric-value">{{ team.storyPointsCommitted }}</div>
            <div class="metric-label">SP Committed</div>
            <div class="metric-sub">At sprint start</div>
          </div>

          <div class="metric-cell">
            <div class="metric-value" style="color:var(--success)">{{ team.storyPointsCompleted }}</div>
            <div class="metric-label">SP Completed</div>
            <div class="metric-sub">Reached DONE</div>
          </div>

          <div class="metric-cell" [class.metric-cell-warn]="team.storyPointsSpilled > 0">
            <div class="metric-value"
                 [style.color]="team.storyPointsSpilled > 0 ? 'var(--warning)' : 'var(--success)'">
              {{ team.storyPointsSpilled }}
            </div>
            <div class="metric-label">SP Spilled</div>
            <div class="metric-sub">Not completed</div>
          </div>

          <div class="metric-cell">
            <div class="metric-value">{{ team.storiesPlanned }}</div>
            <div class="metric-label">Stories Planned</div>
            <div class="metric-sub">At sprint start</div>
          </div>

          <div class="metric-cell">
            <div class="metric-value" style="color:var(--success)">{{ team.storiesAccepted }}</div>
            <div class="metric-label">Stories Accepted</div>
            <div class="metric-sub">Reached DONE</div>
          </div>

          <div class="metric-cell">
            <div class="metric-value">{{ team.capacityPlanned | number:'1.0-0' }}h</div>
            <div class="metric-label">Capacity Planned</div>
            <div class="metric-sub">{{ team.teamSize }} × 60h</div>
          </div>

          <div class="metric-cell">
            <div class="metric-value">{{ team.capacityActual | number:'1.0-0' }}h</div>
            <div class="metric-label">Capacity Actual</div>
            <div class="metric-sub">
              <span *ngIf="team.capacityUtilisation != null">
                {{ team.capacityUtilisation | number:'1.0-0' }}% utilised
              </span>
            </div>
          </div>
        </div>

        <!-- Capacity bar -->
        <div style="padding:0 20px 16px">
          <div class="cap-bar-label">
            Capacity Utilisation
            <span [style.color]="capColor(team.capacityUtilisation)">
              {{ team.capacityUtilisation != null ? (team.capacityUtilisation | number:'1.0-0') + '%' : '—' }}
            </span>
          </div>
          <div class="cap-bar-track">
            <div class="cap-bar-fill"
                 [style.width.%]="Math.min(team.capacityUtilisation ?? 0, 100)"
                 [style.background]="capColor(team.capacityUtilisation)">
            </div>
          </div>
        </div>

        <!-- Expanded section: story tables -->
        <div *ngIf="expandedTeams().has(team.featureTeamId)">

          <!-- Scope creep badge -->
          <div *ngIf="team.storiesAdded > 0"
               class="alert alert-warning" style="margin:0 20px 16px;font-size:13px">
            ⚠️ <strong>Scope Creep:</strong> {{ team.storiesAdded }} {{ team.storiesAdded === 1 ? 'story' : 'stories' }}
            added mid-sprint (+{{ addedPoints(team) }} SP added after sprint start).
          </div>

          <!-- ✅ Stories Completed -->
          <div style="padding:0 20px 20px" *ngIf="team.completedStories.length > 0">
            <div class="story-section-header story-header-success">
              ✅ Stories Completed ({{ team.completedStories.length }})
            </div>
            <table class="story-table">
              <thead>
                <tr>
                  <th>Issue</th>
                  <th>Summary</th>
                  <th>Type</th>
                  <th>SP</th>
                  <th>Epic</th>
                  <th>Assignee</th>
                  <th>Effort Logged</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let s of team.completedStories">
                  <td class="issue-key-cell">{{ s.issueKey }}</td>
                  <td class="summary-cell">{{ s.summary }}</td>
                  <td>
                    <span class="badge" [ngClass]="typeBadge(s.issueType)">{{ s.issueType }}</span>
                  </td>
                  <td class="center">
                    <span class="sp-chip">{{ s.storyPoints ?? '—' }}</span>
                  </td>
                  <td class="text-sm text-secondary">
                    <span *ngIf="s.epicLink" class="epic-tag" [title]="s.epicName">
                      {{ s.epicLink }}
                    </span>
                  </td>
                  <td class="text-sm text-secondary">{{ s.assigneeName || '—' }}</td>
                  <td class="text-sm text-secondary">
                    {{ s.timeSpentMinutes ? (s.timeSpentMinutes | number:'1.0-0') + ' min' : '—' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 🔴 Stories Spilled / Moved Out -->
          <div style="padding:0 20px 20px" *ngIf="team.spilledStories.length > 0">
            <div class="story-section-header story-header-spilled">
              🔴 Stories Spilled / Moved Out ({{ team.spilledStories.length }})
            </div>
            <table class="story-table">
              <thead>
                <tr>
                  <th>Issue</th>
                  <th>Summary</th>
                  <th>Status</th>
                  <th>SP</th>
                  <th>Moved To</th>
                  <th>Spill Reason</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let s of team.spilledStories" class="spilled-row">
                  <td class="issue-key-cell">{{ s.issueKey }}</td>
                  <td class="summary-cell">{{ s.summary }}</td>
                  <td>
                    <span class="badge" [ngClass]="statusBadge(s.status)">
                      {{ statusLabel(s.status) }}
                    </span>
                  </td>
                  <td class="center">
                    <span class="sp-chip sp-chip-spilled">{{ s.storyPoints ?? '—' }}</span>
                  </td>
                  <td class="text-sm text-secondary">
                    <span *ngIf="s.movedToSprint" class="sprint-tag">{{ s.movedToSprint }}</span>
                    <span *ngIf="!s.movedToSprint" class="text-muted">Backlog</span>
                  </td>
                  <td class="text-sm text-secondary spill-reason">
                    {{ s.spillReason || '—' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- ➕ Stories Added Mid-Sprint -->
          <div style="padding:0 20px 20px" *ngIf="team.addedStories.length > 0">
            <div class="story-section-header story-header-added">
              ➕ Added Mid-Sprint ({{ team.addedStories.length }}) — Scope Creep
            </div>
            <table class="story-table">
              <thead>
                <tr>
                  <th>Issue</th>
                  <th>Summary</th>
                  <th>Status</th>
                  <th>SP</th>
                  <th>Assignee</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let s of team.addedStories" class="added-row">
                  <td class="issue-key-cell">{{ s.issueKey }}</td>
                  <td class="summary-cell">{{ s.summary }}</td>
                  <td>
                    <span class="badge badge-success">{{ statusLabel(s.status) }}</span>
                  </td>
                  <td class="center">
                    <span class="sp-chip sp-chip-added">{{ s.storyPoints ?? '—' }}</span>
                  </td>
                  <td class="text-sm text-secondary">{{ s.assigneeName || '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- No stories at all -->
          <div *ngIf="team.completedStories.length === 0 && team.spilledStories.length === 0"
               style="padding:24px 20px;text-align:center;color:var(--text-secondary)">
            No stories recorded for this team in this sprint.
          </div>

        </div><!-- end expanded -->
      </div><!-- end team loop -->

    </ng-container>

    <!-- ── Empty state ────────────────────────────────────────────────────── -->
    <div *ngIf="!loading() && !report()"
         style="text-align:center;padding:64px">
      <div style="font-size:48px;margin-bottom:16px">📋</div>
      <h2 style="font-size:18px;font-weight:600;margin-bottom:8px">No sprint report loaded</h2>
      <p class="text-secondary mb-16">Select a sprint and click Refresh to load the report.</p>
      <button class="btn btn-primary" (click)="load()">Load Report</button>
    </div>

  </div>
  `,
  styles: [`
    /* ── Export menu ───────────────────────────────────────────────────────── */
    .export-menu {
      position:absolute; top:calc(100% + 6px); right:0; z-index:1000;
      background:var(--surface); border:1px solid var(--border);
      border-radius:var(--radius-lg); box-shadow:0 10px 30px rgba(0,0,0,.12);
      width:270px; overflow:hidden;
    }
    .export-menu-header {
      padding:9px 16px 7px; font-size:11px; font-weight:700;
      text-transform:uppercase; letter-spacing:.07em; color:var(--text-muted);
      border-bottom:1px solid var(--border);
    }
    .export-item {
      display:flex; align-items:center; gap:12px;
      width:100%; padding:11px 16px; border:none; background:transparent;
      cursor:pointer; text-align:left; transition:background .12s;
    }
    .export-item:hover { background:var(--bg); }
    .export-icon { font-size:20px; width:26px; text-align:center; flex-shrink:0; }
    .export-label { font-size:13px; font-weight:600; color:var(--text-primary); }
    .export-sub   { font-size:11px; color:var(--text-muted); margin-top:2px; }

    /* ── Programme summary ─────────────────────────────────────────────────── */
    .stat-card-success { border-color:var(--success); }
    .stat-card-danger  { border-color:var(--danger); }
    .stat-card-warning { border-color:var(--warning); }
    .stat-target { font-size:11px; color:var(--text-muted); margin-top:2px; }

    /* ── Predictability bar chart ──────────────────────────────────────────── */
    .pred-bar-row {
      display:flex; align-items:center; gap:12px;
      padding:6px 0; border-bottom:1px solid var(--border);
    }
    .pred-bar-row:last-child { border-bottom:none; }
    .pred-bar-label { width:160px; font-size:13px; font-weight:500;
                      flex-shrink:0; color:var(--text-primary); }
    .pred-bar-track {
      flex:1; height:18px; background:var(--bg); border-radius:9px;
      position:relative; overflow:visible;
    }
    .pred-bar-fill {
      height:100%; border-radius:9px; transition:width .5s ease; min-width:4px;
    }
    .pred-target-line {
      position:absolute; left:80%; top:-4px; bottom:-4px;
      width:2px; background:var(--text-muted); opacity:0.4;
    }
    .pred-bar-value {
      width:110px; flex-shrink:0; font-size:13px; font-weight:700; text-align:right;
    }

    /* ── Team card ─────────────────────────────────────────────────────────── */
    .team-card {
      border:1px solid var(--border); border-radius:var(--radius-lg);
      overflow:hidden; box-shadow:0 1px 3px rgba(0,0,0,.06);
      background:var(--surface);
    }
    .team-card-header {
      display:flex; align-items:center; gap:16px;
      padding:16px 20px;
    }
    .team-header-success { background:#f0fdf4; border-bottom:1px solid #bbf7d0; }
    .team-header-warning { background:#fffbeb; border-bottom:1px solid #fde68a; }
    .team-avatar {
      width:44px; height:44px; border-radius:50%;
      background:var(--primary); color:#fff;
      display:flex; align-items:center; justify-content:center;
      font-weight:700; font-size:15px; flex-shrink:0;
    }
    .team-name { font-size:16px; font-weight:700; color:var(--text-primary); }
    .team-meta { font-size:12px; color:var(--text-secondary); margin-top:2px; }
    .team-summary {
      padding:10px 20px; font-size:13px; color:var(--text-secondary);
      background:var(--bg); border-bottom:1px solid var(--border);
      font-style:italic;
    }

    /* ── Metrics grid ──────────────────────────────────────────────────────── */
    .team-metrics-grid {
      display:grid; grid-template-columns:repeat(auto-fit, minmax(130px,1fr));
      border-bottom:1px solid var(--border);
    }
    .metric-cell {
      padding:16px; text-align:center;
      border-right:1px solid var(--border);
    }
    .metric-cell:last-child { border-right:none; }
    .metric-cell-good { background:#f0fdf4; }
    .metric-cell-bad  { background:#fff5f5; }
    .metric-cell-warn { background:#fffbeb; }
    .metric-value { font-size:28px; font-weight:800; color:var(--text-primary); line-height:1.1; }
    .metric-label { font-size:12px; font-weight:600; color:var(--text-secondary); margin-top:4px; }
    .metric-sub   { font-size:10px; color:var(--text-muted); margin-top:2px; }

    /* ── Capacity bar ──────────────────────────────────────────────────────── */
    .cap-bar-label {
      font-size:12px; font-weight:600; color:var(--text-secondary);
      display:flex; justify-content:space-between; margin-bottom:4px;
    }
    .cap-bar-track {
      height:8px; background:var(--bg); border-radius:9px; overflow:hidden;
    }
    .cap-bar-fill { height:100%; border-radius:9px; transition:width .5s ease; }

    /* ── Trend badge ──────────────────────────────────────────────────────── */
    .trend-badge {
      padding:3px 10px; border-radius:9999px;
      font-size:11px; font-weight:700; text-transform:uppercase;
    }
    .trend-up     { background:#dcfce7; color:#16a34a; }
    .trend-down   { background:#fee2e2; color:#dc2626; }
    .trend-stable { background:#f1f5f9; color:#475569; }
    .trend-unknown{ background:#f1f5f9; color:#94a3b8; }

    /* ── Story tables ──────────────────────────────────────────────────────── */
    .story-section-header {
      font-size:13px; font-weight:700; padding:10px 14px;
      border-radius:var(--radius); margin-bottom:10px;
    }
    .story-header-success { background:#dcfce7; color:#15803d; }
    .story-header-spilled { background:#fee2e2; color:#991b1b; }
    .story-header-added   { background:#fef3c7; color:#92400e; }

    .story-table {
      width:100%; border-collapse:collapse; font-size:12.5px;
    }
    .story-table th {
      text-align:left; padding:8px 12px;
      background:var(--bg); font-size:11px; font-weight:700;
      text-transform:uppercase; letter-spacing:.05em; color:var(--text-muted);
      border-bottom:2px solid var(--border);
    }
    .story-table td {
      padding:9px 12px; border-bottom:1px solid var(--border);
      vertical-align:middle;
    }
    .story-table tr:last-child td { border-bottom:none; }
    .story-table tr:hover td { background:var(--bg); }
    .spilled-row td { background:#fff9f9; }
    .added-row td   { background:#fffdf0; }

    .issue-key-cell { font-family:monospace; font-weight:700; color:var(--primary); white-space:nowrap; }
    .summary-cell   { max-width:320px; }
    .center         { text-align:center; }

    .sp-chip {
      display:inline-flex; align-items:center; justify-content:center;
      width:28px; height:28px; border-radius:50%;
      font-size:12px; font-weight:700;
      background:var(--primary-light); color:var(--primary);
    }
    .sp-chip-spilled { background:#fee2e2; color:#991b1b; }
    .sp-chip-added   { background:#fef3c7; color:#92400e; }

    .epic-tag {
      display:inline-block; padding:2px 7px;
      background:#ede9fe; color:#5b21b6;
      border-radius:4px; font-size:11px; font-weight:600;
    }
    .sprint-tag {
      display:inline-block; padding:2px 7px;
      background:#e0f2fe; color:#0369a1;
      border-radius:4px; font-size:11px; font-weight:600;
    }
    .spill-reason {
      max-width:260px; color:var(--danger); font-style:italic;
    }
  `]
})
export class SprintReportComponent implements OnInit {

  Math = Math; // expose Math to template

  // ── State ─────────────────────────────────────────────────────────────────
  report      = signal<PiSprintReport | null>(null);
  sprints     = signal<JiraSprint[]>([]);
  loading     = signal(false);
  exportOpen  = signal(false);

  // ── Filter state ──────────────────────────────────────────────────────────
  selectedPi        = 'PI-7';
  selectedSprintId: number | null = null;
  teamFilter        = '';
  expandedTeams     = signal<Set<string>>(new Set());

  piOptions = ['PI-5', 'PI-6', 'PI-7', 'PI-8'];
  projectKey = 'COMMSSURV';

  // ── Derived ───────────────────────────────────────────────────────────────
  visibleTeams = () => {
    const teams = this.report()?.teamReports ?? [];
    return this.teamFilter
      ? teams.filter(t => t.featureTeamId === this.teamFilter)
      : teams;
  };

  teamCount    = () => this.report()?.teamReports.length ?? 0;
  teamsAtTarget= () => this.report()?.teamReports.filter(t => t.meetsPredictabilityTarget).length ?? 0;

  programmePredGood = (r: PiSprintReport) =>
    r.programmePredictability != null && r.programmePredictability >= 80;

  addedPoints = (t: TeamSprintReport) =>
    t.addedStories.reduce((s, x) => s + (x.storyPoints ?? 0), 0);

  constructor(
    private jira: JiraService,
    private exportSvc: ExportService
  ) {}

  ngOnInit(): void { this.loadSprints(); this.load(); }

  // ── Data loading ──────────────────────────────────────────────────────────

  loadSprints(): void {
    this.jira.getSprintsForReport(this.projectKey).subscribe({
      next: r => this.sprints.set(r.data),
      error: () => {}
    });
  }

  load(): void {
    this.loading.set(true);
    this.report.set(null);
    this.jira.getSprintReport(
      this.projectKey,
      this.selectedSprintId ?? undefined,
      this.selectedPi
    ).subscribe({
      next: r => {
        this.report.set(r.data);
        this.loading.set(false);
        // Auto-expand all teams on first load
        //const all = new Set(r.data.teamReports.map((t: TeamSprintReport) => t.featureTeamId));
        const all: Set<string> = new Set(
          r.data.teamReports.map((t: TeamSprintReport) => t.featureTeamId as string)
        );
        this.expandedTeams.set(all);
      },
      error: () => this.loading.set(false)
    });
  }

  onSprintChange(id: number | null): void {
    this.selectedSprintId = id;
    this.load();
  }

  // ── Expand / collapse ──────────────────────────────────────────────────────
  toggleTeam(id: string): void {
    const s = new Set(this.expandedTeams());
    s.has(id) ? s.delete(id) : s.add(id);
    this.expandedTeams.set(s);
  }

  // ── Export ─────────────────────────────────────────────────────────────────
  toggleExport(): void { this.exportOpen.set(!this.exportOpen()); }

  exportCsv(): void {
    const r = this.report(); if (!r) return;
    this.buildAndDownloadCsv(r);
    this.exportOpen.set(false);
  }

  exportPdf(): void {
    const r = this.report(); if (!r) return;
    const win = window.open('', '_blank', 'width=1100,height=800');
    if (!win) { alert('Allow pop-ups to export PDF'); return; }
    win.document.write(this.buildHtmlReport(r));
    win.document.close();
    setTimeout(() => { win.focus(); win.print(); }, 600);
    this.exportOpen.set(false);
  }

  exportHtml(): void {
    const r = this.report(); if (!r) return;
    const blob = new Blob([this.buildHtmlReport(r)], { type: 'text/html;charset=utf-8;' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `AgileGuard_SprintReport_${r.sprintName.replace(/\s+/g,'-')}.html`;
    a.click();
    URL.revokeObjectURL(a.href);
    this.exportOpen.set(false);
  }

  // ── CSV builder ────────────────────────────────────────────────────────────
  private buildAndDownloadCsv(r: PiSprintReport): void {
    const rows: string[][] = [];

    // Programme summary
    rows.push(['SPRINT REPORT', r.sprintName]);
    rows.push(['PI', r.piName]);
    rows.push(['Sprint State', r.sprintState]);
    rows.push(['Sprint Goal', r.sprintGoal || '']);
    rows.push(['Start Date', r.sprintStartDate || '']);
    rows.push(['End Date', r.sprintEndDate || '']);
    rows.push(['Programme Predictability (%)', String(r.programmePredictability ?? '')]);
    rows.push(['Total Velocity (SP)', String(r.totalVelocity)]);
    rows.push(['Total Stories Planned', String(r.totalStoriesPlanned)]);
    rows.push(['Total Stories Accepted', String(r.totalStoriesAccepted)]);
    rows.push(['Total Stories Spilled', String(r.totalStoriesSpilled)]);
    rows.push(['Total Capacity Planned (hrs)', String(r.totalCapacityPlanned.toFixed(1))]);
    rows.push(['Total Capacity Actual (hrs)', String(r.totalCapacityActual.toFixed(1))]);
    rows.push([]);

    // Team metrics header
    rows.push([
      'Feature Team','Predictability (%)','Velocity (SP)',
      'SP Committed','SP Completed','SP Spilled',
      'Stories Planned','Stories Accepted','Stories Spilled','Stories Added',
      'Capacity Planned (hrs)','Capacity Actual (hrs)','Capacity Utilisation (%)',
      'Team Size','Meets Target','Velocity Trend'
    ]);

    r.teamReports.forEach(t => {
      rows.push([
        this.esc(t.featureTeamName),
        String(t.predictability ?? ''),
        String(t.velocity),
        String(t.storyPointsCommitted),
        String(t.storyPointsCompleted),
        String(t.storyPointsSpilled),
        String(t.storiesPlanned),
        String(t.storiesAccepted),
        String(t.storiesSpilled),
        String(t.storiesAdded),
        String(t.capacityPlanned.toFixed(1)),
        String(t.capacityActual.toFixed(1)),
        String(t.capacityUtilisation != null ? t.capacityUtilisation.toFixed(1) : ''),
        String(t.teamSize),
        t.meetsPredictabilityTarget ? 'YES' : 'NO',
        t.velocityTrend
      ]);
    });

    rows.push([]);

    // Story detail per team
    r.teamReports.forEach(t => {
      rows.push(['TEAM: ' + t.featureTeamName]);
      rows.push(['COMPLETED STORIES']);
      rows.push(['Issue Key','Summary','Type','Story Points','Epic','Assignee','Effort (min)']);
      t.completedStories.forEach(s => rows.push([
        s.issueKey, this.esc(s.summary), s.issueType,
        String(s.storyPoints ?? ''), s.epicLink || '', s.assigneeName || '',
        String(s.timeSpentMinutes || '')
      ]));
      if (t.spilledStories.length > 0) {
        rows.push([]);
        rows.push(['SPILLED / MOVED OUT STORIES']);
        rows.push(['Issue Key','Summary','Status','Story Points','Moved To Sprint','Spill Reason']);
        t.spilledStories.forEach(s => rows.push([
          s.issueKey, this.esc(s.summary),
          this.statusLabel(s.status),
          String(s.storyPoints ?? ''),
          s.movedToSprint || 'Backlog',
          this.esc(s.spillReason)
        ]));
      }
      if (t.addedStories.length > 0) {
        rows.push([]);
        rows.push(['ADDED MID-SPRINT (SCOPE CREEP)']);
        rows.push(['Issue Key','Summary','Status','Story Points','Assignee']);
        t.addedStories.forEach(s => rows.push([
          s.issueKey, this.esc(s.summary),
          this.statusLabel(s.status),
          String(s.storyPoints ?? ''),
          s.assigneeName || ''
        ]));
      }
      rows.push([]);
    });

    const csv = '\uFEFF' + rows.map(r => r.join(',')).join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `AgileGuard_SprintReport_${r.sprintName.replace(/\s+/g,'-')}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  private esc(v: string | null | undefined): string {
    if (!v) return '';
    return '"' + v.replace(/"/g, '""') + '"';
  }

  // ── HTML report builder ────────────────────────────────────────────────────
  private buildHtmlReport(r: PiSprintReport): string {
    const predColor = (p: number | null) =>
      p == null ? '#64748b' : p >= 80 ? '#16a34a' : p >= 60 ? '#d97706' : '#dc2626';

    const teamRows = r.teamReports.map(t => `
      <tr>
        <td style="font-weight:600">${t.featureTeamName}</td>
        <td style="color:${predColor(t.predictability)};font-weight:700;text-align:center">
          ${t.predictability?.toFixed(1) ?? '—'}%
        </td>
        <td style="text-align:center;font-weight:700;color:#1e2761">${t.velocity}</td>
        <td style="text-align:center">${t.storyPointsCommitted}</td>
        <td style="text-align:center;color:#16a34a">${t.storyPointsCompleted}</td>
        <td style="text-align:center;color:${t.storyPointsSpilled > 0 ? '#d97706' : '#16a34a'}">${t.storyPointsSpilled}</td>
        <td style="text-align:center">${t.storiesPlanned}</td>
        <td style="text-align:center;color:#16a34a">${t.storiesAccepted}</td>
        <td style="text-align:center;color:${t.storyPointsSpilled > 0 ? '#d97706' : '#16a34a'}">${t.storiesSpilled}</td>
        <td style="text-align:center">${t.capacityPlanned.toFixed(0)}h</td>
        <td style="text-align:center">${t.capacityActual.toFixed(0)}h</td>
        <td style="text-align:center">${t.capacityUtilisation?.toFixed(1) ?? '—'}%</td>
        <td style="text-align:center">${t.meetsPredictabilityTarget ? '✅' : '⚠️'}</td>
      </tr>`).join('');

    const storyDetails = r.teamReports.map(t => `
      <h3 style="color:#1e2761;margin:24px 0 8px;font-size:16px">${t.featureTeamName}</h3>
      <p style="font-size:13px;color:#64748b;margin-bottom:8px;font-style:italic">${t.summary || ''}</p>

      <div style="color:#15803d;font-weight:700;font-size:13px;background:#dcfce7;padding:8px 12px;border-radius:6px;margin-bottom:8px">
        ✅ Completed Stories (${t.completedStories.length})
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:12px;margin-bottom:16px">
        <tr style="background:#f8fafc">
          <th style="text-align:left;padding:6px 10px;color:#64748b">Key</th>
          <th style="text-align:left;padding:6px 10px;color:#64748b">Summary</th>
          <th style="text-align:center;padding:6px 10px;color:#64748b">SP</th>
          <th style="text-align:left;padding:6px 10px;color:#64748b">Assignee</th>
        </tr>
        ${t.completedStories.map(s => `
        <tr style="border-top:1px solid #e2e8f0">
          <td style="padding:6px 10px;font-family:monospace;font-weight:600;color:#1e2761">${s.issueKey}</td>
          <td style="padding:6px 10px">${s.summary}</td>
          <td style="padding:6px 10px;text-align:center;font-weight:700">${s.storyPoints ?? '—'}</td>
          <td style="padding:6px 10px;color:#64748b">${s.assigneeName || '—'}</td>
        </tr>`).join('')}
      </table>

      ${t.spilledStories.length > 0 ? `
      <div style="color:#991b1b;font-weight:700;font-size:13px;background:#fee2e2;padding:8px 12px;border-radius:6px;margin-bottom:8px">
        🔴 Spilled / Moved Out (${t.spilledStories.length})
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:12px;margin-bottom:16px">
        <tr style="background:#f8fafc">
          <th style="text-align:left;padding:6px 10px;color:#64748b">Key</th>
          <th style="text-align:left;padding:6px 10px;color:#64748b">Summary</th>
          <th style="text-align:center;padding:6px 10px;color:#64748b">SP</th>
          <th style="text-align:left;padding:6px 10px;color:#64748b">Moved To</th>
          <th style="text-align:left;padding:6px 10px;color:#64748b">Reason</th>
        </tr>
        ${t.spilledStories.map(s => `
        <tr style="border-top:1px solid #e2e8f0;background:#fff5f5">
          <td style="padding:6px 10px;font-family:monospace;font-weight:600;color:#991b1b">${s.issueKey}</td>
          <td style="padding:6px 10px">${s.summary}</td>
          <td style="padding:6px 10px;text-align:center;font-weight:700;color:#dc2626">${s.storyPoints ?? '—'}</td>
          <td style="padding:6px 10px;color:#0369a1">${s.movedToSprint || 'Backlog'}</td>
          <td style="padding:6px 10px;color:#dc2626;font-style:italic">${s.spillReason || '—'}</td>
        </tr>`).join('')}
      </table>` : ''}
    `).join('');

    return `<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8">
<title>AgileGuard — SAFe Sprint Report — ${r.sprintName}</title>
<style>
  * { box-sizing:border-box; margin:0; padding:0; }
  body { font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
         background:#f8fafc; color:#1e293b; line-height:1.5; }
  @media print { body{background:#fff} @page{margin:1.5cm;size:A4} }
  table th, table td { vertical-align:middle; }
</style></head><body>
<div style="max-width:1100px;margin:0 auto;padding:32px 24px">

  <div style="background:linear-gradient(135deg,#1e2761,#283a8e);border-radius:16px;
              padding:28px 32px;margin-bottom:28px;color:#fff">
    <div style="font-size:12px;font-weight:600;color:#cadcfc;letter-spacing:.08em;
                text-transform:uppercase;margin-bottom:6px">🛡️ AgileGuard · SAFe Sprint Report</div>
    <h1 style="font-size:24px;font-weight:800;margin-bottom:4px">${r.sprintName}</h1>
    <div style="font-size:14px;color:#cadcfc">
      ${r.piName} &nbsp;·&nbsp; State: ${r.sprintState}
      ${r.sprintGoal ? `&nbsp;·&nbsp; Goal: ${r.sprintGoal}` : ''}
    </div>
  </div>

  <h2 style="font-size:17px;font-weight:700;color:#1e2761;margin-bottom:14px">Programme Summary</h2>
  <div style="display:grid;grid-template-columns:repeat(6,1fr);gap:12px;margin-bottom:28px">
    ${[
      ['Programme Predictability', r.programmePredictability != null ? r.programmePredictability.toFixed(1)+'%' : '—', predColor(r.programmePredictability)],
      ['Total Velocity (SP)', String(r.totalVelocity), '#1e2761'],
      ['Stories Accepted', String(r.totalStoriesAccepted), '#16a34a'],
      ['Stories Spilled', String(r.totalStoriesSpilled), r.totalStoriesSpilled > 0 ? '#d97706' : '#16a34a'],
      ['Capacity Planned', r.totalCapacityPlanned.toFixed(0)+'h', '#1e2761'],
      ['Capacity Actual', r.totalCapacityActual.toFixed(0)+'h', '#1e2761'],
    ].map(([label, val, color]) => `
      <div style="background:#fff;border:1px solid #e2e8f0;border-radius:10px;padding:16px;text-align:center">
        <div style="font-size:26px;font-weight:800;color:${color}">${val}</div>
        <div style="font-size:11px;color:#64748b;margin-top:3px">${label}</div>
      </div>`).join('')}
  </div>

  <h2 style="font-size:17px;font-weight:700;color:#1e2761;margin-bottom:14px">Team Metrics</h2>
  <div style="background:#fff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;margin-bottom:28px">
    <table style="width:100%;border-collapse:collapse;font-size:12px">
      <thead>
        <tr style="background:#f8fafc;border-bottom:2px solid #e2e8f0">
          <th style="text-align:left;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase;letter-spacing:.05em">Team</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">Predictability</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">Velocity</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">SP Committed</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">SP Completed</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">SP Spilled</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">Planned</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">Accepted</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">Spilled</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">Cap Planned</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">Cap Actual</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">Utilisation</th>
          <th style="text-align:center;padding:10px 14px;color:#64748b;font-size:11px;text-transform:uppercase">On Target</th>
        </tr>
      </thead>
      <tbody>${teamRows}</tbody>
    </table>
  </div>

  <h2 style="font-size:17px;font-weight:700;color:#1e2761;margin-bottom:16px">Team Story Detail</h2>
  ${storyDetails}

  <div style="margin-top:40px;padding-top:16px;border-top:1px solid #e2e8f0;
              font-size:11px;color:#94a3b8;text-align:center">
    Generated by AgileGuard · ${new Date().toLocaleString()}
  </div>
</div></body></html>`;
  }

  // ── Display helpers ────────────────────────────────────────────────────────

  predColor(p: number | null): string {
    if (p == null) return 'var(--text-muted)';
    return p >= 80 ? 'var(--success)' : p >= 60 ? 'var(--warning)' : 'var(--danger)';
  }

  capColor(p: number | null): string {
    if (p == null) return 'var(--text-muted)';
    return p > 100 ? 'var(--danger)' : p >= 80 ? 'var(--success)' : 'var(--warning)';
  }

  teamInitials(name: string): string {
    return name.split(/\s+/).slice(0, 2).map(w => w[0]).join('').toUpperCase();
  }

  sprintStateBadge(state: string): string {
    return state === 'active' ? 'badge-success' : state === 'closed' ? 'badge-gray' : 'badge-info';
  }

  trendClass(t: string): string {
    const map: Record<string, string> = {
      UP: 'trend-up', DOWN: 'trend-down', STABLE: 'trend-stable', UNKNOWN: 'trend-unknown'
    };
    return map[t] ?? 'trend-unknown';
  }

  trendIcon(t: string): string {
    const map: Record<string, string> = {
      UP: '↑', DOWN: '↓', STABLE: '→', UNKNOWN: '?'
    };
    return map[t] ?? '?';
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      DONE: 'Done', IN_PROGRESS: 'In Progress', BLOCKED: 'Blocked',
      READY_FOR_QA: 'Ready for QA', QA_IN_PROGRESS: 'QA In Progress',
      READY_FOR_RELEASE: 'Ready for Release', TO_DO: 'To Do', IN_REVIEW: 'In Review'
    };
    return map[status] ?? status;
  }

  statusBadge(status: string): string {
    const map: Record<string, string> = {
      DONE: 'badge-success', IN_PROGRESS: 'badge-info',
      BLOCKED: 'badge-danger', READY_FOR_QA: 'badge-info',
      QA_IN_PROGRESS: 'badge-warning', READY_FOR_RELEASE: 'badge-info',
      TO_DO: 'badge-gray', IN_REVIEW: 'badge-info'
    };
    return map[status] ?? 'badge-gray';
  }

  typeBadge(type: string): string {
    const map: Record<string, string> = {
      STORY: 'badge-info', BUG: 'badge-danger', TASK: 'badge-gray'
    };
    return map[type] ?? 'badge-gray';
  }
}
