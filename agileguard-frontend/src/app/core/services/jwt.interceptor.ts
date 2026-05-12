import { inject }                        from '@angular/core';
import { HttpInterceptorFn }             from '@angular/common/http';
import { AuthService }                   from '../services/auth.service';

/**
 * Functional HTTP interceptor that attaches the JWT Bearer token
 * to every outgoing request that targets the AgileGuard API.
 *
 * This MUST be registered BEFORE the sessionExpiredInterceptor in
 * app.config.ts so that tokens are attached before 401 checks run.
 *
 * Skips: requests that already have an Authorization header (e.g. the
 * connectivity test endpoints that send personal PAT tokens).
 *
 * Registration order in app.config.ts:
 *   withInterceptors([jwtInterceptor, sessionExpiredInterceptor])
 *                      ↑ first           ↑ second
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth  = inject(AuthService);

  // Skip if already has an Authorization header (personal token tests)
  // Skip external URLs (Confluence, GitHub direct calls) — only patch /api/* routes
  const isApiCall = req.url.startsWith('/api/') || req.url.includes('localhost:808');
  const hasAuth   = req.headers.has('Authorization');

  if (isApiCall && !hasAuth) {
    const token = auth.getAccessToken();
    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
  }

  return next(req);
};
