import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

/**
 * Login page component.
 * Shows demo credentials for local development POC.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:var(--bg)">
      <div style="width:420px">
        <!-- Brand -->
        <div style="text-align:center;margin-bottom:32px">
          <div style="font-size:48px;margin-bottom:8px">🛡️</div>
          <h1 style="font-size:28px;font-weight:700;color:var(--primary)">AgileGuard</h1>
          <p style="color:var(--text-secondary);margin-top:4px">SDLC Governance Platform</p>
        </div>

        <div class="card">
          <h2 style="font-size:20px;font-weight:600;margin-bottom:24px">Sign in</h2>

          <div class="alert alert-info mb-16">
            <strong>Demo accounts:</strong><br>
            Admin: admin&#64;db.com / Admin&#64;1234<br>
            Dev: dev&#64;db.com / Dev&#64;1234<br>
            QA: qa&#64;db.com / Qa&#64;1234
          </div>

          <div *ngIf="error()" class="alert alert-danger mb-16">{{ error() }}</div>

          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-group">
              <label class="form-label required">Email</label>
              <input type="email" class="form-control" formControlName="email"
                     [class.error]="f['email'].invalid && f['email'].touched"
                     placeholder="you@company.com">
              <div class="form-error" *ngIf="f['email'].invalid && f['email'].touched">
                Valid email is required
              </div>
            </div>

            <div class="form-group">
              <label class="form-label required">Password</label>
              <input type="password" class="form-control" formControlName="password"
                     [class.error]="f['password'].invalid && f['password'].touched"
                     placeholder="••••••••">
              <div class="form-error" *ngIf="f['password'].invalid && f['password'].touched">
                Password is required
              </div>
            </div>

            <button type="submit" class="btn btn-primary w-full" [disabled]="loading() || form.invalid"
                    style="justify-content:center;padding:12px">
              <span *ngIf="loading()" class="spinner" style="width:18px;height:18px"></span>
              {{ loading() ? 'Signing in...' : 'Sign in' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  form: FormGroup;
  loading = signal(false);
  error = signal('');

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      email: ['admin@db.com', [Validators.required, Validators.email]],
      password: ['Admin@1234', Validators.required]
    });
  }

  get f() { return this.form.controls; }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.value).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (e) => {
        this.error.set(e.error?.message ?? 'Login failed. Check credentials.');
        this.loading.set(false);
      }
    });
  }
}
