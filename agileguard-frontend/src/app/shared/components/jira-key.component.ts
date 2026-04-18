import {
  Component, Input, OnChanges, signal, computed, inject, HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { JiraConfigService } from '../../core/services/jira-config.service';

/**
 * Renders a JIRA issue key as a clickable button/link that opens in a new tab.
 *
 * Two modes:
 *
 *   CONFIGURED (jiraBaseUrl stored in JiraConfigService)
 *     → renders as <a href="{base}/browse/{key}" target="_blank">
 *     → clicking opens the JIRA story directly
 *
 *   NOT CONFIGURED (first use, or URL cleared)
 *     → renders as a styled button with a "⚙" hint
 *     → clicking opens a tiny inline popup asking for the JIRA base URL
 *     → on Save the URL is persisted and the JIRA story opens immediately
 *
 * Once configured the URL is stored in localStorage ('ag_jira_base_url')
 * and reused on every subsequent visit across all pages — no re-entry needed.
 */
@Component({
  selector: 'app-jira-key',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <!-- CONFIGURED: direct link -->
    <a *ngIf="url(); else unconfigured"
       [href]="url()"
       target="_blank"
       rel="noopener noreferrer"
       class="jk-link"
       [class.jk-epic]="type === 'epic'"
       [title]="label ? label + ' — open in JIRA' : 'Open ' + issueKey + ' in JIRA'">
      {{ issueKey }}<span class="jk-icon">↗</span>
    </a>

    <!-- NOT CONFIGURED: button that opens setup popup -->
    <ng-template #unconfigured>
      <div class="jk-wrapper" (click)="$event.stopPropagation()">
        <button class="jk-link jk-setup"
                [class.jk-epic]="type === 'epic'"
                [title]="'Click to configure JIRA URL and open ' + issueKey"
                (click)="togglePopup()">
          {{ issueKey }}<span class="jk-icon">⚙</span>
        </button>

        <!-- Inline setup popup -->
        <div *ngIf="showPopup()" class="jk-popup">
          <div class="jk-popup-header">
            <span>🔗 Set JIRA Base URL</span>
            <button class="jk-close" (click)="showPopup.set(false)">×</button>
          </div>
          <p class="jk-popup-hint">
            Enter your Atlassian URL once. All JIRA keys will become clickable links.
          </p>
          <input class="jk-input"
                 [(ngModel)]="inputUrl"
                 placeholder="https://yourcompany.atlassian.net"
                 (keydown.enter)="save()"
                 autocomplete="off">
          <div class="jk-popup-error" *ngIf="inputError">{{ inputError }}</div>
          <div class="jk-popup-actions">
            <button class="jk-btn-cancel" (click)="showPopup.set(false)">Cancel</button>
            <button class="jk-btn-save" (click)="save()">Save &amp; Open →</button>
          </div>
        </div>
      </div>
    </ng-template>
  `,
  styles: [`
    :host { display:inline-flex; align-items:center; position:relative; }

    /* ── Shared badge base ──────────────────────────────────────────────── */
    .jk-link {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
      font-size: 12px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
      white-space: nowrap;
      text-decoration: none;
      cursor: pointer;
      border: 1px solid #bae6fd;
      background: #e0f2fe;
      color: #0369a1;
      transition: background .15s, color .15s, border-color .15s;
    }
    .jk-link:hover {
      background: #0369a1;
      color: #ffffff;
      border-color: #0369a1;
    }

    /* Epic variant */
    .jk-epic { background:#ede9fe; color:#5b21b6; border-color:#ddd6fe; }
    .jk-link.jk-epic:hover { background:#5b21b6; color:#fff; border-color:#5b21b6; }

    /* Setup variant — dashed border indicates "needs config" */
    button.jk-link { background:none; }
    .jk-setup { border-style: dashed !important; opacity: .85; }
    .jk-setup:hover { opacity: 1; }

    /* External / gear icon */
    .jk-icon { font-size:10px; opacity:.7; font-style:normal;
               font-family:system-ui,sans-serif; line-height:1; }
    .jk-link:hover .jk-icon { opacity:1; }

    /* ── Wrapper for popup positioning ─────────────────────────────────── */
    .jk-wrapper { position:relative; display:inline-flex; align-items:center; }

    /* ── Setup popup ────────────────────────────────────────────────────── */
    .jk-popup {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      z-index: 9999;
      width: 320px;
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      box-shadow: 0 8px 24px rgba(0,0,0,.15);
      padding: 14px 16px 12px;
      animation: jk-pop .12s ease-out;
    }
    @keyframes jk-pop {
      from { opacity:0; transform:translateY(-4px); }
      to   { opacity:1; transform:translateY(0); }
    }

    .jk-popup-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 13px;
      font-weight: 700;
      color: #1e2761;
      margin-bottom: 6px;
    }
    .jk-close {
      background: none;
      border: none;
      font-size: 18px;
      cursor: pointer;
      color: #94a3b8;
      line-height: 1;
      padding: 0 2px;
    }
    .jk-close:hover { color: #334155; }

    .jk-popup-hint {
      font-size: 12px;
      color: #64748b;
      margin: 0 0 10px;
      line-height: 1.4;
    }

    .jk-input {
      width: 100%;
      box-sizing: border-box;
      padding: 7px 10px;
      font-size: 13px;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      outline: none;
      transition: border-color .15s;
    }
    .jk-input:focus { border-color: #0369a1; }

    .jk-popup-error {
      font-size: 11px;
      color: #dc2626;
      margin-top: 4px;
    }

    .jk-popup-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 10px;
    }
    .jk-btn-cancel {
      padding: 5px 12px;
      font-size: 12px;
      border: 1px solid #e2e8f0;
      border-radius: 5px;
      background: #fff;
      cursor: pointer;
      color: #64748b;
    }
    .jk-btn-cancel:hover { background: #f8fafc; }

    .jk-btn-save {
      padding: 5px 14px;
      font-size: 12px;
      font-weight: 700;
      border: none;
      border-radius: 5px;
      background: #0369a1;
      color: #fff;
      cursor: pointer;
      transition: background .15s;
    }
    .jk-btn-save:hover { background: #0284c7; }
  `]
})
export class JiraKeyComponent implements OnChanges {
  @Input({ required: true }) issueKey!: string;
  @Input() type: 'issue' | 'epic' = 'issue';
  @Input() label = '';

  private jiraConfig = inject(JiraConfigService);

  showPopup = signal(false);
  inputUrl  = '';
  inputError = '';

  // Local signal so computed() tracks issueKey changes
  private _issueKey = signal('');

  /** Returns full JIRA browse URL when configured, or '' when not. */
  url = computed(() => {
    const base = this.jiraConfig.baseUrl();
    const key  = this._issueKey();
    if (!base || !key) return '';
    return `${base}/browse/${key}`;
  });

  ngOnChanges(): void {
    if (this.issueKey !== this._issueKey()) {
      this._issueKey.set(this.issueKey);
    }
  }

  togglePopup(): void {
    this.inputUrl   = this.jiraConfig.baseUrl() || '';
    this.inputError = '';
    this.showPopup.set(!this.showPopup());
  }

  save(): void {
    const raw = (this.inputUrl || '').trim();
    if (!raw) { this.inputError = 'Please enter your JIRA URL.'; return; }
    if (!raw.startsWith('http')) {
      this.inputError = 'URL must start with https://'; return;
    }

    this.jiraConfig.setBaseUrl(raw);
    this.showPopup.set(false);
    this.inputError = '';

    // Open the JIRA story now that we have the URL
    const target = `${this.jiraConfig.baseUrl()}/browse/${this.issueKey}`;
    window.open(target, '_blank', 'noopener,noreferrer');
  }

  /** Close popup when user clicks anywhere outside it. */
  @HostListener('document:click')
  onDocumentClick(): void {
    if (this.showPopup()) { this.showPopup.set(false); }
  }
}