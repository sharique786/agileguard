import { Component, Input, OnChanges, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JiraConfigService } from '../../core/services/jira-config.service';

/**
 * Renders any JIRA issue key as a clickable link that opens in a new browser tab.
 *
 * Reactivity:
 *   - Uses a local WritableSignal (_issueKey) updated in ngOnChanges so that
 *     computed() correctly tracks BOTH the issueKey binding AND the JiraConfigService
 *     baseUrl signal. The link appears/updates automatically without page reload.
 *   - JiraConfigService seeds the base URL from localStorage on boot, so links work
 *     immediately after the first admin panel load (URL persists across sessions).
 *
 * Graceful degradation:
 *   - When no JIRA base URL is configured the key renders as a styled monospace badge
 *     (same look, just not clickable). No broken links or empty hrefs ever shown.
 *
 * Usage:
 *   <app-jira-key [issueKey]="'COMMSSURV-101'" />
 *   <app-jira-key [issueKey]="s.issueKey" [type]="'epic'" [label]="s.summary" />
 */
@Component({
  selector: 'app-jira-key',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container *ngIf="url(); else plain">
      <a [href]="url()"
         target="_blank"
         rel="noopener noreferrer"
         class="jira-key-link"
         [class.jira-key-epic]="type === 'epic'"
         [title]="label || ('Open ' + issueKey + ' in JIRA')">
        {{ issueKey }}
        <span *ngIf="showIcon" class="jira-ext-icon" aria-hidden="true">↗</span>
      </a>
    </ng-container>

    <ng-template #plain>
      <span class="jira-key-plain"
            [class.jira-key-epic]="type === 'epic'"
            [title]="label || issueKey">
        {{ issueKey }}
      </span>
    </ng-template>
  `,
  styles: [`
    :host { display:inline-flex; align-items:center; }

    .jira-key-link, .jira-key-plain {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
      font-size: 12px;
      font-weight: 700;
      padding: 2px 7px;
      border-radius: 4px;
      white-space: nowrap;
      text-decoration: none;
      background: #e0f2fe;
      color: #0369a1;
      border: 1px solid #bae6fd;
      transition: background 0.15s, color 0.15s, border-color 0.15s;
    }

    .jira-key-link:hover {
      background: #0369a1;
      color: #ffffff;
      border-color: #0369a1;
    }

    .jira-key-link.jira-key-epic,
    .jira-key-plain.jira-key-epic {
      background: #ede9fe;
      color: #5b21b6;
      border-color: #ddd6fe;
    }
    .jira-key-link.jira-key-epic:hover {
      background: #5b21b6;
      color: #ffffff;
      border-color: #5b21b6;
    }

    .jira-ext-icon {
      font-size: 10px;
      opacity: 0.7;
      font-style: normal;
      font-family: system-ui, sans-serif;
      line-height: 1;
    }
    .jira-key-link:hover .jira-ext-icon { opacity: 1; }

    .jira-key-plain { cursor: default; }
  `]
})
export class JiraKeyComponent implements OnChanges {
  @Input({ required: true }) issueKey!: string;
  @Input() type: 'issue' | 'epic' = 'issue';
  @Input() label = '';
  @Input() showIcon = true;

  private jiraConfig = inject(JiraConfigService);

  /**
   * Local signal so computed() can track issueKey as a reactive dependency.
   * Angular's computed() only tracks Signals — plain @Input properties are
   * invisible to it, so we mirror the @Input into a signal via ngOnChanges.
   */
  private _issueKey = signal('');

  /**
   * Computed URL — re-evaluates whenever either:
   *   _issueKey signal changes  (issueKey @Input changed by parent)
   *   jiraConfig.baseUrl signal changes  (admin panel loaded tenant, or localStorage)
   */
  url = computed(() => {
    const base = this.jiraConfig.baseUrl();
    const key  = this._issueKey();
    if (!base || !key) return '';
    return `${base}/browse/${key}`;
  });

  ngOnChanges(): void {
    // Mirror the @Input into the signal on every change-detection cycle
    if (this.issueKey !== this._issueKey()) {
      this._issueKey.set(this.issueKey);
    }
  }
}
