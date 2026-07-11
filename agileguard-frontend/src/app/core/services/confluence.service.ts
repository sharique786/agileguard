import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError } from 'rxjs';

export interface ConfluencePageContent {
  pageId:     string;
  title:      string;
  spaceKey:   string;
  url:        string;
  bodyText:   string;   // plain text extracted from the page body
  excerpt:    string;   // first 500 chars for display
  fetchedAt:  string;
}

/**
 * Fetches Confluence page content for LLM context.
 * The backend proxy calls Confluence REST API v2 using the tenant's
 * Confluence credentials and returns extracted plain text.
 *
 * Confluence URL patterns handled:
 *   https://{site}.atlassian.net/wiki/spaces/{space}/pages/{id}/{title}
 *   https://{site}.atlassian.net/wiki/pages/viewpage.action?pageId={id}
 */
@Injectable({ providedIn: 'root' })
export class ConfluenceService {

  loading  = signal(false);
  lastPage = signal<ConfluencePageContent | null>(null);
  error    = signal('');

  private base = '/api/confluence';

  constructor(private http: HttpClient) {}

  /**
   * Fetches and parses a Confluence page by its full URL.
   * Returns the extracted plain text to be embedded in the AI prompt.
   */
  fetchPage(confluenceUrl: string): Observable<any> {
    this.loading.set(true); this.error.set(''); this.lastPage.set(null);
    return this.http.post<any>(`${this.base}/fetch`, { url: confluenceUrl }).pipe(
      catchError(err => of({ success: false, error: err?.error?.message ?? 'Failed to fetch Confluence page.' }))
    );
  }

  /** Convenience — fetches and stores result in signal. */
  fetchAndStore(confluenceUrl: string): void {
    this.fetchPage(confluenceUrl).subscribe(r => {
      this.loading.set(false);
      if (r?.data) {
        this.lastPage.set(r.data);
      } else {
        this.error.set(r?.message ?? r?.error ?? 'Could not fetch page.');
      }
    });
  }

  /** Builds a prompt-ready context string from a page result. */
  toContext(page: ConfluencePageContent): string {
    return `CONFLUENCE PAGE CONTEXT:\nTitle: ${page.title}\nSpace: ${page.spaceKey}\nURL: ${page.url}\n\n${page.bodyText}`;
  }

  clearPage(): void { this.lastPage.set(null); this.error.set(''); }
}
