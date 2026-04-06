import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Restricts access to admin routes (onboarding, admin panel).
 * Allowed roles: SUPER_ADMIN, TENANT_ADMIN, PROJECT_ADMIN.
 * All other authenticated users are redirected to /dashboard.
 */
export const adminGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) return router.createUrlTree(['/login']);
  const role = auth.getRole();
  const allowed = ['SUPER_ADMIN', 'TENANT_ADMIN', 'PROJECT_ADMIN'];
  if (allowed.includes(role)) return true;
  return router.createUrlTree(['/dashboard']);
};
