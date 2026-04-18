import { Injectable, signal } from '@angular/core';

/**
 * Singleton service that stores the tenant's JIRA base URL.
 *
 * Persistence: stores in localStorage key 'ag_jira_base_url' so the URL
 * survives page refresh and only needs to be entered once.
 *
 * JIRA browse URL pattern:
 *   {baseUrl}/browse/{issueKey}
 *   e.g. https://acme.atlassian.net/browse/COMMSSURV-101
 *
 * The JiraKeyComponent reads baseUrl as a reactive signal — any update
 * (from Admin panel, AppComponent startup, or the inline setup popup)
 * immediately makes all badges on every page clickable without a reload.
 */
@Injectable({ providedIn: 'root' })
export class JiraConfigService {

  private readonly STORAGE_KEY = 'ag_jira_base_url';

  /** Reactive signal — JiraKeyComponent reads this in computed(). */
  baseUrl = signal<string>(this.loadFromStorage());

  /**
   * Persist and activate a new JIRA base URL.
   * Called from:
   *   - AppComponent.ngOnInit()   (seeded from tenant API on every login)
   *   - AdminComponent            (when admin saves JIRA config)
   *   - JiraKeyComponent.save()   (inline first-time setup popup)
   */
  setBaseUrl(url: string): void {
    if (!url) return;
    const clean = url.replace(/\/$/, '').trim(); // strip trailing slash
    localStorage.setItem(this.STORAGE_KEY, clean);
    this.baseUrl.set(clean);
  }

  /** True when a JIRA base URL has been configured. */
  isConfigured(): boolean {
    return !!this.baseUrl();
  }

  /** Builds a full browse URL. Returns '' if not configured. */
  browseUrl(issueKey: string): string {
    const base = this.baseUrl();
    return base && issueKey ? `${base}/browse/${issueKey}` : '';
  }

  /** Clears the stored URL (e.g. when tenant changes). */
  clearUrl(): void {
    localStorage.removeItem(this.STORAGE_KEY);
    this.baseUrl.set('');
  }

  private loadFromStorage(): string {
    try {
      return localStorage.getItem(this.STORAGE_KEY) ?? '';
    } catch {
      return ''; // SSR / private-mode safety
    }
  }
}