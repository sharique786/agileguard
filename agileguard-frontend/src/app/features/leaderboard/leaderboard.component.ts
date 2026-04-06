import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GitHubService } from '../../core/services/github.service';
import { LeaderboardEntry } from '../../core/models';

/** Developer and QA leaderboard showing GitHub + JIRA composite scores. */
@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div>
      <div class="page-header">
        <h1 class="page-title">🏆 Team Leaderboard</h1>
        <p class="page-subtitle">GitHub + JIRA composite score • Current sprint</p>
      </div>

      <div *ngIf="loading()" style="text-align:center;padding:48px">
        <div class="spinner" style="width:40px;height:40px;margin:0 auto 16px"></div>
        <p class="text-secondary">Computing leaderboard...</p>
      </div>

      <div *ngIf="!loading()">
        <!-- Top 3 podium -->
        <div class="flex gap-16 mb-24" style="justify-content:center">
          <div *ngFor="let entry of top3()" class="card" style="text-align:center;flex:0 0 200px">
            <div style="font-size:32px;margin-bottom:8px">{{ podiumIcon(entry.rank) }}</div>
            <div class="lb-avatar" style="margin:0 auto 12px">{{ initials(entry.fullName) }}</div>
            <div style="font-weight:700;font-size:15px">{{ entry.fullName }}</div>
            <div class="text-secondary text-sm">{{ entry.featureTeam }}</div>
            <div style="font-size:28px;font-weight:800;color:var(--primary);margin:8px 0">
              {{ entry.compositeScore }}
            </div>
            <div class="flex gap-4" style="flex-wrap:wrap;justify-content:center">
              <span *ngFor="let b of entry.badges" class="badge badge-purple" style="font-size:10px">{{ b }}</span>
            </div>
          </div>
        </div>

        <!-- Full table -->
        <div class="card">
          <div class="card-title">Full Rankings</div>
          <table class="table">
            <thead>
              <tr>
                <th>Rank</th><th>Contributor</th><th>Team</th><th>Role</th>
                <th>Score</th><th>Commits</th><th>PR Reviews</th><th>CI Pass%</th>
                <th>Stories</th><th>Avg Quality</th><th>Badges</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let e of entries()">
                <td>
                  <span class="lb-rank" [class.top3]="e.rank <= 3">
                    {{ e.rank <= 3 ? podiumIcon(e.rank) : '#' + e.rank }}
                  </span>
                </td>
                <td>
                  <div class="flex items-center gap-8">
                    <div class="lb-avatar" style="width:32px;height:32px;font-size:13px">{{ initials(e.fullName) }}</div>
                    <div>
                      <div style="font-weight:500;font-size:13px">{{ e.fullName }}</div>
                      <div style="font-size:11px;color:var(--text-muted)">{{ e.githubUsername }}</div>
                    </div>
                  </div>
                </td>
                <td class="text-sm">{{ e.featureTeam }}</td>
                <td><span class="badge badge-gray text-xs">{{ e.role }}</span></td>
                <td>
                  <div class="flex items-center gap-8">
                    <strong style="font-size:16px" [style.color]="scoreColor(e.compositeScore)">{{ e.compositeScore }}</strong>
                    <div class="health-bar-wrap" style="width:60px">
                      <div class="health-bar" [style.width.%]="e.compositeScore"
                           [style.background]="scoreColor(e.compositeScore)"></div>
                    </div>
                  </div>
                </td>
                <td class="text-sm">{{ e.commitCount }}</td>
                <td class="text-sm">{{ e.prReviewCount }}</td>
                <td>
                  <span class="badge" [ngClass]="e.ciPassRate >= 90 ? 'badge-success' : e.ciPassRate >= 75 ? 'badge-warning' : 'badge-danger'">
                    {{ e.ciPassRate }}%
                  </span>
                </td>
                <td class="text-sm">{{ e.storiesDelivered }}</td>
                <td class="text-sm">{{ e.avgStoryQuality | number:'1.1-1' }}</td>
                <td>
                  <span *ngFor="let b of e.badges" class="badge badge-purple text-xs" style="margin-right:4px">{{ b }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Score explanation -->
        <div class="card mt-16">
          <div class="card-title">Score Breakdown</div>
          <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px">
            <div *ngFor="let item of scoreBreakdown" class="flex items-center gap-12">
              <div style="width:44px;height:44px;border-radius:50%;background:var(--primary-light);
                          display:flex;align-items:center;justify-content:center;font-size:20px;flex-shrink:0">
                {{ item.icon }}
              </div>
              <div>
                <div style="font-weight:600;font-size:13px">{{ item.label }}</div>
                <div style="font-size:12px;color:var(--text-secondary)">{{ item.weight }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class LeaderboardComponent implements OnInit {
  entries = signal<LeaderboardEntry[]>([]);
  loading = signal(true);

  scoreBreakdown = [
    { icon: '📝', label: 'Story Quality', weight: '30% — Avg Gemini AI score' },
    { icon: '⏱️', label: 'Effort Logging', weight: '20% — Daily log compliance' },
    { icon: '✅', label: 'Sub-task Compliance', weight: '15% — Dev + QA subtasks' },
    { icon: '🔍', label: 'PR Reviews', weight: '15% — Peer review contributions' },
    { icon: '⚡', label: 'CI Pass Rate', weight: '10% — Pipeline success %' },
    { icon: '💻', label: 'Commit Frequency', weight: '10% — Sprint commits' }
  ];

  constructor(private github: GitHubService) {}

  ngOnInit(): void {
    this.github.getLeaderboard('proj-001').subscribe({
      next: (res) => { this.entries.set(res.data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  top3() { return this.entries().slice(0, Math.min(3, this.entries().length)); }
  initials(name: string) { return name.split(' ').map(n => n[0]).join('').toUpperCase(); }
  podiumIcon(rank: number) { return ['🥇','🥈','🥉'][rank-1] ?? '#' + rank; }
  scoreColor(s: number) { return s >= 80 ? 'var(--success)' : s >= 60 ? 'var(--warning)' : 'var(--danger)'; }
}
