import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },

  // Public
  { path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },

  // Public — new tenant onboarding wizard (no auth required)
  { path: 'onboarding',
    loadComponent: () => import('./features/onboarding/onboarding.component').then(m => m.OnboardingComponent) },

  // Protected — authenticated users
  { path: 'dashboard',    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
  { path: 'stories',      canActivate: [authGuard],
    loadComponent: () => import('./features/story-form/story-form.component').then(m => m.StoryFormComponent) },
  { path: 'reports',      canActivate: [authGuard],
    loadComponent: () => import('./features/reports/reports.component').then(m => m.ReportsComponent) },
  { path: 'leaderboard',  canActivate: [authGuard],
    loadComponent: () => import('./features/leaderboard/leaderboard.component').then(m => m.LeaderboardComponent) },

  // Admin only — TENANT_ADMIN, PROJECT_ADMIN, SUPER_ADMIN
  { path: 'admin',        canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin.component').then(m => m.AdminComponent) },

  { path: '**', redirectTo: '/dashboard' }
];
