import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError, map } from 'rxjs';

/**
 * Manages personal JIRA and GitHub API tokens for the current browser SESSION.
 * Uses sessionStorage — tokens are cleared automatically when the tab closes.
 * Tokens are never persisted to localStorage or sent to any AgileGuard DB table.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly JIRA_KEY    = 'ag_jira_pat';
  private readonly GITHUB_KEY  = 'ag_github_pat';
  private readonly JIRA_URL_KEY= 'ag_jira_personal_url';

  jiraToken   = signal<string>(sessionStorage.getItem(this.JIRA_KEY)    ?? '');
  githubToken = signal<string>(sessionStorage.getItem(this.GITHUB_KEY)  ?? '');
  jiraUrl     = signal<string>(sessionStorage.getItem(this.JIRA_URL_KEY) ?? '');

  jiraStatus   = signal<'idle'|'testing'|'ok'|'fail'>('idle');
  githubStatus = signal<'idle'|'testing'|'ok'|'fail'>('idle');
  jiraMessage   = signal('');
  githubMessage = signal('');

  constructor(private http: HttpClient) {}

  setJira(token: string, url: string): void {
    const t = token.trim(); const u = url.replace(/\/$/, '').trim();
    sessionStorage.setItem(this.JIRA_KEY,     t);
    sessionStorage.setItem(this.JIRA_URL_KEY, u);
    this.jiraToken.set(t); this.jiraUrl.set(u); this.jiraStatus.set('idle'); this.jiraMessage.set('');
  }

  setGithub(token: string): void {
    sessionStorage.setItem(this.GITHUB_KEY, token.trim());
    this.githubToken.set(token.trim()); this.githubStatus.set('idle'); this.githubMessage.set('');
  }

  clearAll(): void {
    [this.JIRA_KEY, this.GITHUB_KEY, this.JIRA_URL_KEY].forEach(k => sessionStorage.removeItem(k));
    this.jiraToken.set(''); this.githubToken.set(''); this.jiraUrl.set('');
    this.jiraStatus.set('idle'); this.githubStatus.set('idle');
    this.jiraMessage.set(''); this.githubMessage.set('');
  }

  hasJira():   boolean { return !!(this.jiraToken() && this.jiraUrl()); }
  hasGithub(): boolean { return !!this.githubToken(); }

  testJira(): void {
    if (!this.hasJira()) { this.jiraStatus.set('fail'); this.jiraMessage.set('Enter JIRA URL and token first.'); return; }
    this.jiraStatus.set('testing'); this.jiraMessage.set('');
    this.http.post<any>('/api/integrations/test/jira', { jiraBaseUrl: this.jiraUrl(), token: this.jiraToken() }).pipe(
      catchError(err => of({ data: { connected: false }, message: err?.error?.message ?? 'Network error' }))
    ).subscribe(r => {
      const ok = r?.data?.connected === true;
      this.jiraStatus.set(ok ? 'ok' : 'fail');
      this.jiraMessage.set(ok ? `Connected as ${r.data.displayName ?? 'user'}` : (r?.message ?? 'Failed'));
    });
  }

  testGithub(): void {
    if (!this.hasGithub()) { this.githubStatus.set('fail'); this.githubMessage.set('Enter a GitHub token first.'); return; }
    this.githubStatus.set('testing'); this.githubMessage.set('');
    this.http.post<any>('/api/integrations/test/github', { token: this.githubToken() }).pipe(
      catchError(err => of({ data: { connected: false }, message: err?.error?.message ?? 'Network error' }))
    ).subscribe(r => {
      const ok = r?.data?.connected === true;
      this.githubStatus.set(ok ? 'ok' : 'fail');
      this.githubMessage.set(ok ? `Connected as @${r.data.login ?? 'user'}` : (r?.message ?? 'Failed'));
    });
  }
}
