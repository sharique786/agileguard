import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, ValidationResult } from '../models';

/** Service for Gemini AI validation and suggestion endpoints. */
@Injectable({ providedIn: 'root' })
export class AiService {
  private base = `${environment.apiUrl}/ai`;

  constructor(private http: HttpClient) {}

  validate(story: { title: string; description: string; acceptanceCriteria: string; issueType: string; storyPoints?: number }): Observable<ApiResponse<ValidationResult>> {
    return this.http.post<ApiResponse<ValidationResult>>(`${this.base}/validate`, story);
  }

  generateAC(title: string, description: string): Observable<ApiResponse<string[]>> {
    return this.http.post<ApiResponse<string[]>>(`${this.base}/ac/generate`, { title, description });
  }

  /** Connects to the SSE stream for real-time validation feedback. */
  streamValidation(title: string, description: string, ac: string): Observable<any> {
    const subject = new Subject<any>();
    const url = `${this.base}/validate/stream?title=${encodeURIComponent(title)}&description=${encodeURIComponent(description)}&ac=${encodeURIComponent(ac)}`;
    const es = new EventSource(url);
    es.onmessage = (event) => {
      try { subject.next(JSON.parse(event.data)); } catch { subject.next(event.data); }
    };
    es.onerror = () => { subject.complete(); es.close(); };
    return subject.asObservable();
  }
}
