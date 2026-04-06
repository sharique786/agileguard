import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, WorkflowRun, SdlcStatus, LeaderboardEntry } from '../models';

/** Service for GitHub Actions and leaderboard API calls. */
@Injectable({ providedIn: 'root' })
export class GitHubService {
  private base = `${environment.apiUrl}/github`;

  constructor(private http: HttpClient) {}

  getWorkflowRuns(owner: string, repo: string): Observable<ApiResponse<WorkflowRun[]>> {
    return this.http.get<ApiResponse<WorkflowRun[]>>(`${this.base}/repos/${owner}/${repo}/runs`);
  }

  getSdlcStatus(issueKey: string): Observable<ApiResponse<SdlcStatus>> {
    return this.http.get<ApiResponse<SdlcStatus>>(`${this.base}/sdlc/${issueKey}`);
  }

  getLeaderboard(projectId: string): Observable<ApiResponse<LeaderboardEntry[]>> {
    return this.http.get<ApiResponse<LeaderboardEntry[]>>(`${this.base}/leaderboard/${projectId}`);
  }
}
