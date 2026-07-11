import { Injectable, signal } from '@angular/core';
import { HttpClient }         from '@angular/common/http';
import { Router }             from '@angular/router';
import { tap }                from 'rxjs/operators';
import { Observable }         from 'rxjs';

export interface UserInfo {
  userId:    string;
  tenantId:  string;
  fullName:  string;
  email:     string;
  role:      string;
}

/**
 * Authentication service.
 *
 * Token storage:
 *   - accessToken  → sessionStorage (cleared on tab close)
 *   - refreshToken → sessionStorage (cleared on tab close)
 *   - userInfo     → sessionStorage (cleared on tab close)
 *
 * Using sessionStorage (not localStorage) so tokens are never shared
 * across browser tabs and are automatically cleared when the tab closes.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly ACCESS_KEY  = 'ag_access_token';
  private readonly REFRESH_KEY = 'ag_refresh_token';
  private readonly USER_KEY    = 'ag_user_info';

  /** Reactive signal — components bind to this for current user info. */
  currentUser = signal<UserInfo | null>(this.loadUser());

  constructor(private http: HttpClient, private router: Router) {}

  // ── Login / Logout ─────────────────────────────────────────────────────────

  /**
   * Accepts a single credentials object { email, password } so that
   * login.component.ts can call:  this.auth.login(this.form.value)
   */
  login(credentials: { email: string; password: string }): Observable<any> {
    return this.http.post<any>('/api/auth/login', credentials).pipe(
      tap(r => {
        if (r?.data?.accessToken) {
          this.storeTokens(r.data.accessToken, r.data.refreshToken, r.data.user);
        }
      })
    );
  }

  /**
   * Public method called by onboarding.component after self-registration.
   * Accepts the flat AuthResponse shape returned by the onboarding API:
   *   { accessToken, refreshToken, userId, email, fullName, role, tenantId, ... }
   * Stores tokens + user info so the new admin is logged in immediately.
   */
  storeAuth(tokens: {
    accessToken:  string;
    refreshToken: string;
    userId:       string;
    email:        string;
    fullName:     string;
    role:         string;
    tenantId:     string;
    // optional extras returned by the backend
    tokenType?:   string;
    expiresIn?:   number;
  }): void {
    if (!tokens?.accessToken) return;
    // Map the flat AuthResponse fields into the internal UserInfo shape
    const user: UserInfo = {
      userId:   tokens.userId,
      tenantId: tokens.tenantId,
      fullName: tokens.fullName,
      email:    tokens.email,
      role:     tokens.role,
    };
    this.storeTokens(tokens.accessToken, tokens.refreshToken, user);
  }

  logout(): void {
    sessionStorage.removeItem(this.ACCESS_KEY);
    sessionStorage.removeItem(this.REFRESH_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  // ── Token accessors used by interceptors ───────────────────────────────────

  /**
   * Returns the current access token, or null if not logged in.
   * Called by jwtInterceptor on every outgoing request.
   */
  getAccessToken(): string | null {
    return sessionStorage.getItem(this.ACCESS_KEY);
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem(this.REFRESH_KEY);
  }

  /**
   * Returns true ONLY when a valid access token exists in storage.
   * Used by sessionExpiredInterceptor to distinguish "never logged in"
   * from "was logged in but session expired".
   */
  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  // ── User info helpers ──────────────────────────────────────────────────────

  /** Returns the current user's RBAC role. Safe to call even when logged out. */
  getRole(): string {
    return this.currentUser()?.role ?? '';
  }

  /** Returns the current user's tenantId. Safe to call even when logged out. */
  getTenantId(): string | null {
    return this.currentUser()?.tenantId ?? null;
  }

  getUserId(): string | null {
    return this.currentUser()?.userId ?? null;
  }

  // ── Token refresh ──────────────────────────────────────────────────────────

  refreshAccessToken(): Observable<any> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<any>('/api/auth/refresh', { refreshToken }).pipe(
      tap(r => {
        if (r?.data?.accessToken) {
          sessionStorage.setItem(this.ACCESS_KEY, r.data.accessToken);
        }
      })
    );
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private storeTokens(accessToken: string, refreshToken: string, user: UserInfo): void {
    sessionStorage.setItem(this.ACCESS_KEY,  accessToken);
    sessionStorage.setItem(this.REFRESH_KEY, refreshToken);
    sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private loadUser(): UserInfo | null {
    try {
      const raw = sessionStorage.getItem(this.USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }
}