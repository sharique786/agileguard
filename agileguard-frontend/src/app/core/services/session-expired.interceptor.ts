import { inject }                                from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse }  from '@angular/common/http';
import { catchError }                            from 'rxjs/operators';
import { throwError }                            from 'rxjs';
import { AuthService }                           from '../services/auth.service';
import { SessionExpiredService }                 from './session-expired.service';

/**
 * Functional HTTP interceptor — catches 401 Unauthorized responses and
 * shows the session-expired modal ONLY when a user is genuinely logged in.
 *
 * Guards against false positives:
 *   1. Skips /api/auth/** entirely (login, refresh — expected to return 401)
 *   2. Checks AuthService.isLoggedIn() before triggering — so that background
 *      API calls made before login (e.g. seedJiraUrl) never fire the modal
 *   3. Checks AuthService.getAccessToken() — if no token exists at all, the
 *      request was unauthenticated by design; don't show session expired
 *
 * MUST be registered AFTER jwtInterceptor in app.config.ts so it sees the
 * request with the token already attached.
 */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const auth       = inject(AuthService);
  const sessionSvc = inject(SessionExpiredService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        const isAuthEndpoint  = req.url.includes('/api/auth/');
        const isLoggedIn      = auth.isLoggedIn();
        const hasToken        = !!auth.getAccessToken();

        // Only trigger the session-expired modal when:
        // - This is NOT a login/refresh/register endpoint
        // - The user IS currently logged in (they had a valid session)
        // - They had a token (confirms they were authenticated, not anonymous)
        if (!isAuthEndpoint && isLoggedIn && hasToken) {
          sessionSvc.triggerExpiry();
        }
      }
      return throwError(() => err);
    })
  );
};
