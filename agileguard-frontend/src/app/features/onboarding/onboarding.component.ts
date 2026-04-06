import { Component, signal, computed, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { TenantService } from '../../core/services/tenant.service';
import { AuthService } from '../../core/services/auth.service';

/**
 * 5-step tenant onboarding wizard.
 *
 * Step 1 — Organisation     name, slug (live availability check), plan
 * Step 2 — JIRA connection  baseUrl, email, token, test-connection button
 * Step 3 — Project & Teams  project team name/key, add multiple feature teams
 * Step 4 — Admin user       email, full name, password, confirm password
 * Step 5 — Review & Submit  read-only summary → POST /api/onboarding
 */
@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div style="min-height:100vh;background:var(--bg);display:flex;align-items:center;
                justify-content:center;padding:32px 16px">
      <div style="width:100%;max-width:760px">

        <!-- Brand header -->
        <div style="text-align:center;margin-bottom:32px">
          <div style="font-size:48px">🛡️</div>
          <h1 style="font-size:28px;font-weight:700;color:var(--primary);margin:8px 0 4px">
            Welcome to AgileGuard
          </h1>
          <p style="color:var(--text-secondary);font-size:15px">
            Set up your organisation in under 5 minutes
          </p>
        </div>

        <!-- Step progress bar -->
        <div class="step-bar">
          <div *ngFor="let s of steps; let i = index"
               class="step-item" [class.active]="currentStep() === i"
               [class.done]="currentStep() > i">
            <div class="step-circle">
              <span *ngIf="currentStep() > i">✓</span>
              <span *ngIf="currentStep() <= i">{{ i + 1 }}</span>
            </div>
            <span class="step-label">{{ s }}</span>
          </div>
          <div class="step-connector"></div>
        </div>

        <!-- ── Card ─────────────────────────────────────────────────── -->
        <div class="card" style="margin-top:24px;padding:32px">

          <!-- ══ Step 1 — Organisation ═══════════════════════════════ -->
          <ng-container *ngIf="currentStep() === 0">
            <h2 class="step-title">🏢 Organisation Details</h2>
            <p class="step-sub">Tell us about your company or team.</p>

            <form [formGroup]="orgForm">
              <div class="form-group">
                <label class="form-label required">Organisation Name</label>
                <input class="form-control" formControlName="name"
                       [class.error]="isInvalid(orgForm, 'name')"
                       placeholder="e.g. Deutsche Bank">
                <div class="form-error" *ngIf="isInvalid(orgForm,'name')">
                  Organisation name is required (2–100 characters)
                </div>
              </div>

              <div class="form-group">
                <label class="form-label required">
                  Slug
                  <span class="text-muted text-xs" style="font-weight:400">
                    — used in URLs, lowercase letters, numbers and hyphens only
                  </span>
                </label>
                <div style="position:relative">
                  <input class="form-control" formControlName="slug"
                         [class.error]="isInvalid(orgForm,'slug') || slugTaken()"
                         placeholder="db-corp"
                         style="padding-right:40px">
                  <span style="position:absolute;right:12px;top:50%;transform:translateY(-50%);font-size:16px">
                    {{ slugChecking() ? '⏳' : slugTaken() ? '❌' : orgForm.get('slug')?.value ? '✅' : '' }}
                  </span>
                </div>
                <div class="form-error" *ngIf="isInvalid(orgForm,'slug')">
                  Slug must be lowercase letters, numbers and hyphens (2–50 chars)
                </div>
                <div class="form-error" *ngIf="slugTaken()">
                  This slug is already taken — please choose another
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">Plan</label>
                <div class="plan-grid">
                  <div *ngFor="let p of plans"
                       class="plan-card"
                       [class.selected]="orgForm.get('plan')?.value === p.value"
                       (click)="orgForm.get('plan')?.setValue(p.value)">
                    <div class="plan-icon">{{ p.icon }}</div>
                    <div class="plan-name">{{ p.label }}</div>
                    <div class="plan-desc">{{ p.desc }}</div>
                  </div>
                </div>
              </div>
            </form>
          </ng-container>

          <!-- ══ Step 2 — JIRA Connection ════════════════════════════ -->
          <ng-container *ngIf="currentStep() === 1">
            <h2 class="step-title">🔗 JIRA Connection</h2>
            <p class="step-sub">
              Connect your Atlassian JIRA workspace.
              You can skip this step and configure it later from the admin panel.
            </p>

            <div class="alert alert-info mb-16" style="font-size:13px">
              💡 Your JIRA API token is encrypted before storage.
              Create one at <strong>id.atlassian.com → Security → API tokens</strong>.
            </div>

            <form [formGroup]="jiraForm">
              <div class="form-group">
                <label class="form-label">JIRA Base URL</label>
                <input class="form-control" formControlName="jiraBaseUrl"
                       placeholder="https://your-org.atlassian.net">
              </div>
              <div class="form-group">
                <label class="form-label">Service Account Email</label>
                <input class="form-control" type="email" formControlName="jiraUserEmail"
                       placeholder="svc-agileguard@yourcompany.com">
              </div>
              <div class="form-group">
                <label class="form-label">API Token</label>
                <input class="form-control" type="password" formControlName="jiraApiToken"
                       placeholder="ATATT3xFfGF…">
              </div>
              <div class="flex gap-8 items-center">
                <button class="btn btn-secondary" type="button"
                        (click)="testJira()" [disabled]="jiraTesting() || !canTestJira()">
                  <span *ngIf="jiraTesting()" class="spinner" style="width:14px;height:14px"></span>
                  {{ jiraTesting() ? 'Testing…' : '⚡ Test Connection' }}
                </button>
                <div *ngIf="jiraResult()" class="flex items-center gap-6">
                  <span>{{ jiraResult()!.connected ? '✅' : '❌' }}</span>
                  <span style="font-size:13px"
                        [style.color]="jiraResult()!.connected ? 'var(--success)' : 'var(--danger)'">
                    {{ jiraResult()!.message }}
                    <ng-container *ngIf="jiraResult()!.connected">
                      — {{ jiraResult()!.displayName }}
                    </ng-container>
                  </span>
                </div>
              </div>
              <div *ngIf="jiraResult()?.connected && jiraResult()!.accessibleProjects?.length"
                   class="mt-8 text-sm text-secondary">
                Accessible projects:
                <span *ngFor="let p of jiraResult()!.accessibleProjects"
                      class="badge badge-info" style="margin-left:4px">{{ p }}</span>
              </div>
            </form>
          </ng-container>

          <!-- ══ Step 3 — Project & Feature Teams ════════════════════ -->
          <ng-container *ngIf="currentStep() === 2">
            <h2 class="step-title">📁 Project &amp; Feature Teams</h2>
            <p class="step-sub">Define your first project and the feature teams within it.</p>

            <form [formGroup]="teamsForm">
              <div class="section-label">Project Team</div>

              <div class="grid-2" style="gap:16px">
                <div class="form-group">
                  <label class="form-label required">Project Team Name</label>
                  <input class="form-control" formControlName="projectName"
                         [class.error]="isInvalid(teamsForm,'projectName')"
                         placeholder="Sigma Team">
                  <div class="form-error" *ngIf="isInvalid(teamsForm,'projectName')">
                    Project team name is required
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-label">JIRA Project Key</label>
                  <input class="form-control" formControlName="jiraProjectKey"
                         placeholder="COMMSSURV"
                         style="text-transform:uppercase"
                         (input)="uppercaseKey($event)">
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">GitHub Organisation</label>
                <input class="form-control" formControlName="githubOrg"
                       placeholder="db-platform">
              </div>

              <!-- Feature teams -->
              <div class="section-label mt-16">
                Feature Teams
                <button type="button" class="btn btn-secondary btn-sm"
                        style="margin-left:12px" (click)="addFeatureTeam()">
                  + Add Team
                </button>
              </div>

              <div formArrayName="featureTeams">
                <div *ngFor="let ft of featureTeamsArray.controls; let i = index"
                     [formGroupName]="i" class="feature-team-row">
                  <div class="grid-2" style="gap:12px;flex:1">
                    <div class="form-group" style="margin-bottom:0">
                      <input class="form-control" formControlName="name"
                             [class.error]="isGroupInvalid(ft,'name')"
                             [placeholder]="'Feature Team ' + (i + 1)">
                      <div class="form-error" *ngIf="isGroupInvalid(ft,'name')">
                        Team name is required
                      </div>
                    </div>
                    <div class="form-group" style="margin-bottom:0">
                      <input class="form-control" formControlName="jiraComponent"
                             placeholder="JIRA Component (optional)">
                    </div>
                  </div>
                  <div class="form-group" style="margin-bottom:0;width:200px">
                    <input class="form-control" formControlName="githubRepos"
                           placeholder="repo1,repo2">
                  </div>
                  <button type="button" class="btn btn-danger btn-sm"
                          (click)="removeFeatureTeam(i)"
                          *ngIf="featureTeamsArray.length > 0">✕</button>
                </div>
              </div>
              <p class="text-sm text-muted mt-8" *ngIf="featureTeamsArray.length === 0">
                No feature teams added yet — you can add them later from the admin panel.
              </p>
            </form>
          </ng-container>

          <!-- ══ Step 4 — Admin User ══════════════════════════════════ -->
          <ng-container *ngIf="currentStep() === 3">
            <h2 class="step-title">👤 Admin Account</h2>
            <p class="step-sub">
              This account will be the Tenant Admin — the first user in your organisation.
            </p>

            <form [formGroup]="adminForm">
              <div class="form-group">
                <label class="form-label required">Full Name</label>
                <input class="form-control" formControlName="fullName"
                       [class.error]="isInvalid(adminForm,'fullName')"
                       placeholder="Jane Smith">
                <div class="form-error" *ngIf="isInvalid(adminForm,'fullName')">
                  Full name is required
                </div>
              </div>

              <div class="form-group">
                <label class="form-label required">Email Address</label>
                <input class="form-control" type="email" formControlName="email"
                       [class.error]="isInvalid(adminForm,'email')"
                       placeholder="jane@yourcompany.com">
                <div class="form-error" *ngIf="isInvalid(adminForm,'email')">
                  A valid email address is required
                </div>
              </div>

              <div class="grid-2">
                <div class="form-group">
                  <label class="form-label required">Password</label>
                  <input class="form-control" type="password" formControlName="password"
                         [class.error]="isInvalid(adminForm,'password')"
                         placeholder="Min. 8 characters">
                  <div class="form-error" *ngIf="isInvalid(adminForm,'password')">
                    Password must be at least 8 characters
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-label required">Confirm Password</label>
                  <input class="form-control" type="password" formControlName="confirmPassword"
                         [class.error]="passwordMismatch()"
                         placeholder="Repeat password">
                  <div class="form-error" *ngIf="passwordMismatch()">
                    Passwords do not match
                  </div>
                </div>
              </div>

              <div class="grid-2">
                <div class="form-group">
                  <label class="form-label">GitHub Username</label>
                  <input class="form-control" formControlName="githubUsername"
                         placeholder="jane-dev">
                </div>
                <div class="form-group">
                  <label class="form-label">JIRA Account ID</label>
                  <input class="form-control" formControlName="jiraAccountId"
                         placeholder="5f3e... (from Atlassian profile)">
                </div>
              </div>

              <!-- Password strength indicator -->
              <div *ngIf="adminForm.get('password')?.value" style="margin-top:-8px;margin-bottom:16px">
                <div class="health-bar-wrap" style="height:5px">
                  <div class="health-bar" [style.width.%]="passwordStrength() * 25"
                       [style.background]="passwordStrength() >= 4 ? 'var(--success)'
                                         : passwordStrength() >= 3 ? 'var(--warning)'
                                         : 'var(--danger)'"></div>
                </div>
                <div style="font-size:11px;color:var(--text-muted);margin-top:2px">
                  Strength: {{ ['Very weak','Weak','Fair','Strong','Very strong'][passwordStrength() - 1] || 'Very weak' }}
                </div>
              </div>
            </form>
          </ng-container>

          <!-- ══ Step 5 — Review ═════════════════════════════════════ -->
          <ng-container *ngIf="currentStep() === 4">
            <h2 class="step-title">✅ Review &amp; Confirm</h2>
            <p class="step-sub">Please review your setup before creating the organisation.</p>

            <div class="review-section">
              <div class="review-header">🏢 Organisation</div>
              <div class="review-row"><span>Name</span><strong>{{ orgForm.value.name }}</strong></div>
              <div class="review-row"><span>Slug</span><code>{{ orgForm.value.slug }}</code></div>
              <div class="review-row"><span>Plan</span>
                <span class="badge badge-purple">{{ orgForm.value.plan }}</span>
              </div>
            </div>

            <div class="review-section" *ngIf="jiraForm.value.jiraBaseUrl">
              <div class="review-header">🔗 JIRA</div>
              <div class="review-row"><span>Workspace</span><strong>{{ jiraForm.value.jiraBaseUrl }}</strong></div>
              <div class="review-row"><span>Service Account</span><strong>{{ jiraForm.value.jiraUserEmail }}</strong></div>
              <div class="review-row"><span>API Token</span><span class="text-muted">••••••••••</span></div>
              <div class="review-row" *ngIf="jiraResult()?.connected">
                <span>Status</span>
                <span class="badge badge-success">✅ Connected — {{ jiraResult()!.displayName }}</span>
              </div>
            </div>
            <div class="review-section" *ngIf="!jiraForm.value.jiraBaseUrl">
              <div class="review-header">🔗 JIRA</div>
              <div class="review-row"><span>Status</span>
                <span class="badge badge-gray">Skipped — configure later</span>
              </div>
            </div>

            <div class="review-section">
              <div class="review-header">📁 Project Team</div>
              <div class="review-row"><span>Name</span><strong>{{ teamsForm.value.projectName }}</strong></div>
              <div class="review-row" *ngIf="teamsForm.value.jiraProjectKey">
                <span>JIRA Key</span><code>{{ teamsForm.value.jiraProjectKey }}</code>
              </div>
              <div class="review-row" *ngIf="teamsForm.value.githubOrg">
                <span>GitHub Org</span><strong>{{ teamsForm.value.githubOrg }}</strong>
              </div>
            </div>

            <div class="review-section" *ngIf="featureTeamsArray.length > 0">
              <div class="review-header">👥 Feature Teams</div>
              <div *ngFor="let ft of featureTeamsArray.value; let i=index" class="review-row">
                <span>Team {{ i + 1 }}</span>
                <div>
                  <strong>{{ ft.name }}</strong>
                  <span *ngIf="ft.jiraComponent" class="text-muted text-sm"> ({{ ft.jiraComponent }})</span>
                </div>
              </div>
            </div>

            <div class="review-section">
              <div class="review-header">👤 Admin User</div>
              <div class="review-row"><span>Name</span><strong>{{ adminForm.value.fullName }}</strong></div>
              <div class="review-row"><span>Email</span><strong>{{ adminForm.value.email }}</strong></div>
              <div class="review-row"><span>Role</span><span class="badge badge-purple">TENANT_ADMIN</span></div>
            </div>

            <div *ngIf="submitError()" class="alert alert-danger mt-16">{{ submitError() }}</div>
          </ng-container>

          <!-- ── Navigation buttons ────────────────────────────────── -->
          <div class="flex justify-between items-center mt-24">
            <button class="btn btn-secondary"
                    *ngIf="currentStep() > 0"
                    (click)="prev()" [disabled]="submitting()">
              ← Back
            </button>
            <div *ngIf="currentStep() === 0"></div>

            <button class="btn btn-primary"
                    *ngIf="currentStep() < 4"
                    (click)="next()" [disabled]="!canProceed()">
              Next →
            </button>

            <button class="btn btn-primary"
                    *ngIf="currentStep() === 4"
                    (click)="submit()" [disabled]="submitting()">
              <span *ngIf="submitting()" class="spinner" style="width:16px;height:16px"></span>
              {{ submitting() ? 'Creating organisation…' : '🚀 Create Organisation' }}
            </button>
          </div>
        </div>

        <p style="text-align:center;margin-top:20px;font-size:13px;color:var(--text-muted)">
          Already have an account?
          <a routerLink="/login" style="color:var(--primary);font-weight:500;text-decoration:none">
            Sign in
          </a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .step-bar { display:flex;align-items:center;justify-content:center;gap:0;position:relative;margin-bottom:8px; }
    .step-connector { position:absolute;top:17px;left:10%;right:10%;height:2px;background:var(--border);z-index:0; }
    .step-item { display:flex;flex-direction:column;align-items:center;gap:6px;flex:1;z-index:1; }
    .step-circle { width:34px;height:34px;border-radius:50%;display:flex;align-items:center;
                   justify-content:center;font-size:13px;font-weight:700;border:2px solid var(--border);
                   background:var(--surface);color:var(--text-muted);transition:all .2s; }
    .step-item.active .step-circle { border-color:var(--primary);background:var(--primary);color:#fff; }
    .step-item.done   .step-circle { border-color:var(--success);background:var(--success);color:#fff; }
    .step-label { font-size:11px;color:var(--text-muted);white-space:nowrap;font-weight:500; }
    .step-item.active .step-label { color:var(--primary); }
    .step-item.done   .step-label { color:var(--success); }
    .step-title { font-size:22px;font-weight:700;color:var(--text-primary);margin-bottom:6px; }
    .step-sub   { font-size:14px;color:var(--text-secondary);margin-bottom:24px; }
    .section-label { font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.06em;
                     color:var(--text-muted);margin-bottom:12px; }
    .plan-grid { display:grid;grid-template-columns:repeat(3,1fr);gap:12px; }
    .plan-card { border:2px solid var(--border);border-radius:var(--radius-lg);padding:16px;
                 text-align:center;cursor:pointer;transition:all .15s; }
    .plan-card:hover { border-color:var(--primary); }
    .plan-card.selected { border-color:var(--primary);background:var(--primary-light); }
    .plan-icon { font-size:24px;margin-bottom:4px; }
    .plan-name { font-weight:600;font-size:14px;color:var(--text-primary); }
    .plan-desc { font-size:11px;color:var(--text-muted);margin-top:2px; }
    .feature-team-row { display:flex;align-items:flex-start;gap:10px;
                        padding:12px;background:var(--bg);border-radius:var(--radius);
                        margin-bottom:10px;flex-wrap:wrap; }
    .review-section { border:1px solid var(--border);border-radius:var(--radius-lg);
                      overflow:hidden;margin-bottom:16px; }
    .review-header  { background:var(--bg);padding:10px 16px;font-weight:600;
                      font-size:13px;color:var(--text-primary);border-bottom:1px solid var(--border); }
    .review-row { display:flex;align-items:center;justify-content:space-between;
                  padding:10px 16px;font-size:13px;border-bottom:1px solid var(--border); }
    .review-row:last-child { border-bottom:none; }
    .review-row > span:first-child { color:var(--text-secondary);min-width:130px; }
  `]
})
export class OnboardingComponent implements OnDestroy {

  // ── Step config ───────────────────────────────────────────────────────────
  steps = ['Organisation', 'JIRA', 'Teams', 'Admin User', 'Review'];
  currentStep = signal(0);

  plans = [
    { value: 'FREE',       icon: '🆓', label: 'Free',       desc: 'Up to 3 users' },
    { value: 'PRO',        icon: '⚡', label: 'Pro',        desc: 'Up to 25 users' },
    { value: 'ENTERPRISE', icon: '🏢', label: 'Enterprise', desc: 'Unlimited users' },
  ];

  // ── Forms ─────────────────────────────────────────────────────────────────
  orgForm   = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]{2,50}$/)]],
    plan: ['FREE']
  });

  jiraForm  = this.fb.group({
    jiraBaseUrl:   [''],
    jiraUserEmail: [''],
    jiraApiToken:  ['']
  });

  teamsForm = this.fb.group({
    projectName:    ['', [Validators.required, Validators.minLength(2)]],
    jiraProjectKey: [''],
    githubOrg:      [''],
    featureTeams:   this.fb.array([])
  });

  adminForm = this.fb.group({
    fullName:        ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email:           ['', [Validators.required, Validators.email]],
    password:        ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
    githubUsername:  [''],
    jiraAccountId:   ['']
  });

  // ── Async state ───────────────────────────────────────────────────────────
  slugChecking = signal(false);
  slugTaken    = signal(false);
  jiraTesting  = signal(false);
  jiraResult   = signal<any>(null);
  submitting   = signal(false);
  submitError  = signal('');

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private tenantSvc: TenantService,
    private authSvc: AuthService,
    private router: Router
  ) {
    // Auto-generate slug from name
    this.orgForm.get('name')!.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(name => {
        if (!this.orgForm.get('slug')?.dirty) {
          const slug = (name ?? '').toLowerCase()
            .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
          this.orgForm.get('slug')?.setValue(slug, { emitEvent: false });
        }
      });

    // Slug availability check (debounced)
    this.orgForm.get('slug')!.valueChanges.pipe(
      debounceTime(500), distinctUntilChanged(), takeUntil(this.destroy$)
    ).subscribe(slug => {
      if (slug && this.orgForm.get('slug')!.valid) this.checkSlug(slug);
    });
  }

  get featureTeamsArray(): FormArray {
    return this.teamsForm.get('featureTeams') as FormArray;
  }

  // ── Step navigation ───────────────────────────────────────────────────────

  next(): void {
    if (this.canProceed()) this.currentStep.update(s => s + 1);
  }

  prev(): void { this.currentStep.update(s => Math.max(0, s - 1)); }

  canProceed(): boolean {
    switch (this.currentStep()) {
      case 0: return this.orgForm.valid && !this.slugTaken();
      case 1: return true; // JIRA step is optional
      case 2: return this.teamsForm.get('projectName')!.valid &&
                     this.featureTeamsArray.controls.every(c => c.valid);
      case 3: return this.adminForm.get('fullName')!.valid &&
                     this.adminForm.get('email')!.valid &&
                     this.adminForm.get('password')!.valid &&
                     !this.passwordMismatch();
      case 4: return true;
      default: return false;
    }
  }

  // ── Slug check ────────────────────────────────────────────────────────────
  private checkSlug(slug: string): void {
    this.slugChecking.set(true);
    this.tenantSvc.checkSlug(slug).subscribe({
      next:  res => { this.slugTaken.set(!res.data); this.slugChecking.set(false); },
      error: ()  => { this.slugChecking.set(false); }
    });
  }

  // ── JIRA test ─────────────────────────────────────────────────────────────
  canTestJira(): boolean {
    const v = this.jiraForm.value;
    return !!(v.jiraBaseUrl && v.jiraUserEmail && v.jiraApiToken);
  }

  testJira(): void {
    if (!this.canTestJira()) return;
    const v = this.jiraForm.value;
    this.jiraTesting.set(true);
    this.tenantSvc.testJiraConnection({
      baseUrl: v.jiraBaseUrl!, userEmail: v.jiraUserEmail!, apiToken: v.jiraApiToken!
    }).subscribe({
      next:  res => { this.jiraResult.set(res.data); this.jiraTesting.set(false); },
      error: err => {
        this.jiraResult.set({ connected: false, message: err.error?.message ?? 'Connection failed' });
        this.jiraTesting.set(false);
      }
    });
  }

  // ── Feature teams ─────────────────────────────────────────────────────────
  addFeatureTeam(): void {
    this.featureTeamsArray.push(this.fb.group({
      name:          ['', Validators.required],
      jiraComponent: [''],
      githubRepos:   [''],
      description:   ['']
    }));
  }

  removeFeatureTeam(index: number): void { this.featureTeamsArray.removeAt(index); }

  // ── Submit ────────────────────────────────────────────────────────────────
  submit(): void {
    this.submitting.set(true);
    this.submitError.set('');
    const ov = this.orgForm.value;
    const jv = this.jiraForm.value;
    const tv = this.teamsForm.value;
    const av = this.adminForm.value;

    const req = {
      organisation: {
        name: ov.name!, slug: ov.slug!, plan: ov.plan ?? 'FREE',
        ...(jv.jiraBaseUrl   ? { jiraBaseUrl:   jv.jiraBaseUrl }   : {}),
        ...(jv.jiraUserEmail ? { jiraUserEmail: jv.jiraUserEmail } : {}),
        ...(jv.jiraApiToken  ? { jiraApiToken:  jv.jiraApiToken }  : {}),
      },
      projectTeam: {
        name: tv.projectName!, jiraProjectKey: tv.jiraProjectKey ?? undefined,
        githubOrg: tv.githubOrg ?? undefined
      },
      featureTeams: (tv.featureTeams as any[]).map(ft => ({
        name: ft.name, jiraComponent: ft.jiraComponent || undefined,
        githubRepos: ft.githubRepos || undefined
      })),
      adminUser: {
        email: av.email!, fullName: av.fullName!, password: av.password!,
        githubUsername: av.githubUsername || undefined,
        jiraAccountId:  av.jiraAccountId  || undefined
      }
    };

    this.tenantSvc.onboard(req).subscribe({
      next: res => {
        // Store JWT tokens so admin is logged in immediately
        const tokens = res.data.tokens;
        this.authSvc['storeAuth']?.(tokens); // calls private method via bracket notation
        this.router.navigate(['/admin'], {
          queryParams: { tenantId: res.data.tenantId, welcome: '1' }
        });
      },
      error: err => {
        this.submitError.set(err.error?.message ?? 'Onboarding failed. Please try again.');
        this.submitting.set(false);
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  isInvalid(form: FormGroup, field: string): boolean {
    const c = form.get(field);
    return !!(c?.invalid && c?.touched);
  }

  isGroupInvalid(ctrl: AbstractControl, field: string): boolean {
    const c = (ctrl as FormGroup).get(field);
    return !!(c?.invalid && c?.touched);
  }

  passwordMismatch(): boolean {
    const f = this.adminForm;
    return f.get('confirmPassword')!.touched &&
           f.get('password')!.value !== f.get('confirmPassword')!.value;
  }

  passwordStrength(): number {
    const pw = this.adminForm.get('password')?.value ?? '';
    let score = 0;
    if (pw.length >= 8)  score++;
    if (pw.length >= 12) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;
    return Math.min(5, score);
  }

  uppercaseKey(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.toUpperCase();
    this.teamsForm.get('jiraProjectKey')?.setValue(input.value, { emitEvent: false });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}
