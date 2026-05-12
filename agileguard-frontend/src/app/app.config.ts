import { ApplicationConfig }                    from '@angular/core';
import { provideRouter }                        from '@angular/router';
import { provideHttpClient, withInterceptors }  from '@angular/common/http';
import { routes }                               from './app.routes';
import { jwtInterceptor }                       from './core/services/jwt.interceptor';
import { sessionExpiredInterceptor }            from './core/services/session-expired.interceptor';

/**
 * Interceptor execution order (top → bottom):
 *
 *   1. jwtInterceptor           — attaches Authorization: Bearer <token>
 *   2. sessionExpiredInterceptor — catches 401s AFTER token is attached
 *
 * Order matters: if sessionExpiredInterceptor ran first, it would see
 * requests without a token and trigger false-positive session expired modals.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        jwtInterceptor,           // ← FIRST: attach token
        sessionExpiredInterceptor // ← SECOND: catch expired sessions
      ])
    ),
  ],
};
