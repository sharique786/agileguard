import { Component, signal, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { AiService }        from '../../core/services/ai.service';
import { JiraService }      from '../../core/services/jira.service';
import { TypeaheadComponent } from '../../shared/components/typeahead.component';
import { TagInputComponent }  from '../../shared/components/tag-input.component';
import {
  ValidationResult, JiraSprint, JiraComponent, JiraVersion, JiraUser, JiraEpic
} from '../../core/models';

/** Default JIRA project key — replace with whatever your workspace uses */
const DEFAULT_PROJECT = 'COMMSSURV';

/**
 * Fully enhanced JIRA Story Editor.
 *
 * New fields (all wired to typeahead or smart dropdowns):
 *   Priority       — dropdown  (Blocker → Trivial)
 *   Business Line  — dropdown  (Business | GT | Platform | …)
 *   Epic Link      — free text → auto-resolves to epic name on blur
 *   Sprint         — typeahead single-select (active + future sprints)
 *   Component/s    — typeahead multi-select
 *   Fix Version/s  — typeahead multi-select
 *   Reporter       — typeahead single-select (JIRA users)
 *   Labels         — tag input (Enter / comma separated)
 *   Team Names     — dropdown multi-select
 *
 * All fields participate in the updated checklist and AI validation payload.
 */
@Component({
  selector: 'app-story-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TypeaheadComponent, TagInputComponent],
  template: `
  <div>
    <div class="page-header flex justify-between items-center">
      <div>
        <h1 class="page-title">📝 Story Editor</h1>
        <p class="page-subtitle">
          Create JIRA stories with AI-powered quality validation
          <span class="badge badge-info" style="margin-left:8px">Project: {{ projectKey }}</span>
        </p>
      </div>
      <button class="btn btn-secondary btn-sm" (click)="resetForm()">↺ Clear form</button>
    </div>

    <!-- Two-column layout: form left, AI panel right -->
    <div style="display:grid;grid-template-columns:1fr 380px;gap:24px;align-items:start">

      <!-- ═══════════════════════════════════════════════════════════
           LEFT — Story form
           ═══════════════════════════════════════════════════════════ -->
      <form [formGroup]="form" (ngSubmit)="onSubmit()">

        <!-- ── Section 1: Core Classification ──────────────────── -->
        <div class="card mb-16">
          <div class="section-head">📋 Classification</div>

          <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px">

            <div class="form-group">
              <label class="form-label required">Issue Type</label>
              <select class="form-control" formControlName="issueType">
                <option value="Story">Story</option>
                <option value="Bug">Bug</option>
                <option value="Task">Task</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label required">Priority</label>
              <select class="form-control" formControlName="priority"
                      [class.error]="isInvalid('priority')">
                <option value="">— Select —</option>
                <option *ngFor="let p of priorities" [value]="p.value">
                  {{ p.icon }} {{ p.label }}
                </option>
              </select>
              <div class="form-error" *ngIf="isInvalid('priority')">Priority is required</div>
            </div>

            <div class="form-group">
              <label class="form-label required">Business Line</label>
              <select class="form-control" formControlName="businessLine"
                      [class.error]="isInvalid('businessLine')">
                <option value="">— Select —</option>
                <option *ngFor="let b of businessLines" [value]="b.value">{{ b.label }}</option>
              </select>
              <div class="form-error" *ngIf="isInvalid('businessLine')">Business Line is required</div>
            </div>

          </div>

          <!-- Team Names multi-select -->
          <div class="form-group">
            <label class="form-label">Team Names</label>
            <div class="team-grid">
              <label *ngFor="let t of teamOptions" class="team-option"
                     [class.selected]="selectedTeams().includes(t)">
                <input type="checkbox"
                       [checked]="selectedTeams().includes(t)"
                       (change)="toggleTeam(t)"
                       style="margin-right:6px">
                {{ t }}
              </label>
            </div>
          </div>

        </div>

        <!-- ── Section 2: Summary & Description ────────────────── -->
        <div class="card mb-16">
          <div class="section-head">✍️ Details</div>

          <div class="form-group">
            <label class="form-label required">Summary / Title</label>
            <input type="text" class="form-control" formControlName="title"
                   [class.error]="isInvalid('title')"
                   placeholder="As a user, I want to…">
            <div class="form-error" *ngIf="isInvalid('title')">
              Title is required (min 5 characters)
            </div>
          </div>

          <div class="form-group">
            <label class="form-label required">
              Description
              <span class="text-secondary text-xs" style="font-weight:400"> — What &amp; Why, not How</span>
            </label>
            <textarea class="form-control" formControlName="description" rows="4"
                      [class.error]="isInvalid('description')"
                      placeholder="As a [user type], I want [goal] so that [benefit]…"></textarea>
            <div class="form-error" *ngIf="isInvalid('description')">
              Description is required (min 20 characters)
            </div>
          </div>

          <div class="form-group">
            <label class="form-label required">
              Acceptance Criteria
              <button type="button" class="btn btn-secondary btn-sm"
                      style="margin-left:8px;font-size:11px"
                      (click)="generateAC()" [disabled]="generatingAC()">
                {{ generatingAC() ? '⏳ Generating…' : '✨ AI Generate' }}
              </button>
            </label>
            <textarea class="form-control" formControlName="acceptanceCriteria" rows="5"
                      [class.error]="isInvalid('acceptanceCriteria')"
                      placeholder="Given [context],&#10;When [action],&#10;Then [expected outcome]"></textarea>
            <div class="form-error" *ngIf="isInvalid('acceptanceCriteria')">
              Acceptance Criteria is required
            </div>
          </div>
        </div>

        <!-- ── Section 3: Planning ──────────────────────────────── -->
        <div class="card mb-16">
          <div class="section-head">📅 Planning</div>

          <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">

            <!-- Story Points -->
            <div class="form-group">
              <label class="form-label">Story Points</label>
              <select class="form-control" formControlName="storyPoints">
                <option value="">— Not estimated —</option>
                <option *ngFor="let p of [1,2,3,5,8,13,21]" [value]="p">{{ p }}</option>
              </select>
            </div>

            <!-- Reporter typeahead -->
            <div class="form-group">
              <app-typeahead
                label="Reporter"
                hint="search by name or email"
                placeholder="Search reporter…"
                displayKey="displayName"
                subKey="emailAddress"
                [items]="userSuggestions()"
                [loading]="usersLoading()"
                (queryChange)="onUserQuery($event)"
                (selected)="onReporterSelected($event)">
              </app-typeahead>
            </div>

          </div>

          <!-- Sprint typeahead -->
          <div class="form-group">
            <app-typeahead
              label="Sprint"
              hint="active &amp; upcoming sprints"
              placeholder="Search or select a sprint…"
              displayKey="name"
              [items]="sprintSuggestions()"
              [loading]="sprintsLoading()"
              (queryChange)="onSprintQuery($event)"
              (selected)="onSprintSelected($event)">
            </app-typeahead>
            <div *ngIf="selectedSprint()" class="field-hint">
              <span class="badge" [ngClass]="sprintBadge(selectedSprint()!.state)">
                {{ selectedSprint()!.state }}
              </span>
              <span *ngIf="selectedSprint()!.endDate" class="text-muted text-xs">
                Ends {{ selectedSprint()!.endDate | date:'d MMM' }}
              </span>
              <span *ngIf="selectedSprint()!.goal" class="text-muted text-xs">
                · Goal: {{ selectedSprint()!.goal }}
              </span>
            </div>
          </div>

          <!-- Epic Link -->
          <div class="form-group">
            <label class="form-label">
              Epic Link
              <span class="text-muted text-xs" style="font-weight:400"> — enter an Epic issue key (e.g. COMMSSURV-5)</span>
            </label>
            <div style="display:flex;gap:10px;align-items:flex-start">
              <input type="text" class="form-control" formControlName="epicLink"
                     style="font-family:monospace;text-transform:uppercase"
                     placeholder="COMMSSURV-5"
                     (blur)="resolveEpic()"
                     (input)="onEpicInput($event)">
              <button type="button" class="btn btn-secondary btn-sm"
                      style="white-space:nowrap;flex-shrink:0;margin-top:0"
                      [disabled]="epicLoading() || !form.value.epicLink"
                      (click)="resolveEpic()">
                <span *ngIf="epicLoading()" class="spinner" style="width:12px;height:12px"></span>
                {{ epicLoading() ? '' : '🔍 Resolve' }}
              </button>
            </div>
            <!-- Resolved Epic card -->
            <div *ngIf="resolvedEpic() as epic" class="epic-card mt-8">
              <div class="epic-key">{{ epic.issueKey }}</div>
              <div class="epic-name">{{ epic.name }}</div>
              <span class="badge badge-purple" style="font-size:11px">Epic</span>
              <button type="button" class="btn-icon" (click)="clearEpic()">×</button>
            </div>
            <!-- Not found warning -->
            <div *ngIf="epicNotFound()" class="alert alert-danger" style="margin-top:6px;padding:6px 10px;font-size:12px">
              ❌ Epic "{{ form.value.epicLink }}" not found or is not an Epic issue type
            </div>
          </div>

        </div>

        <!-- ── Section 4: Metadata ──────────────────────────────── -->
        <div class="card mb-16">
          <div class="section-head">🏷️ Metadata</div>

          <!-- Component/s typeahead multi-select -->
          <div class="form-group">
            <app-typeahead
              label="Component/s"
              mode="multi"
              displayKey="name"
              subKey="description"
              placeholder="Search components…"
              [items]="componentSuggestions()"
              [loading]="componentsLoading()"
              (queryChange)="onComponentQuery($event)"
              (multiSelected)="onComponentsChanged($event)">
            </app-typeahead>
          </div>

          <!-- Fix Version/s typeahead multi-select -->
          <div class="form-group">
            <app-typeahead
              label="Fix Version/s"
              mode="multi"
              displayKey="name"
              subKey="description"
              placeholder="Search fix versions…"
              [items]="versionSuggestions()"
              [loading]="versionsLoading()"
              (queryChange)="onVersionQuery($event)"
              (multiSelected)="onVersionsChanged($event)">
            </app-typeahead>
            <!-- Show release dates for selected versions -->
            <div *ngIf="selectedVersions().length > 0" class="field-hint">
              <span *ngFor="let v of selectedVersions()" class="badge badge-gray" style="margin-right:4px">
                {{ v.name }}
                <span *ngIf="v.releaseDate" style="font-weight:400"> · {{ v.releaseDate | date:'d MMM' }}</span>
              </span>
            </div>
          </div>

          <!-- Labels free-text tag input -->
          <div class="form-group">
            <app-tag-input
              label="Labels"
              placeholder="Add a label…"
              [tags]="selectedLabels()"
              (tagsChange)="onLabelsChanged($event)">
            </app-tag-input>
          </div>

        </div>

        <!-- ── Submit bar ────────────────────────────────────────── -->
        <div class="flex gap-8">
          <button type="submit" class="btn btn-primary"
                  style="flex:1;justify-content:center"
                  [disabled]="saving() || form.invalid">
            <span *ngIf="saving()" class="spinner" style="width:16px;height:16px"></span>
            {{ saving() ? 'Saving…' : '💾 Create Story' }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="runValidation()">
            🤖 AI Review
          </button>
        </div>

        <div class="alert alert-success mt-16" *ngIf="saveSuccess()">
          ✅ Story <strong>{{ savedKey() }}</strong> created successfully!
        </div>
        <div class="alert alert-danger mt-16" *ngIf="saveError()">
          ❌ {{ saveError() }}
        </div>

      </form>

      <!-- ═══════════════════════════════════════════════════════════
           RIGHT — AI Panel + Checklist
           ═══════════════════════════════════════════════════════════ -->
      <div>

        <!-- AI Score Panel -->
        <div class="ai-panel mb-16">
          <div class="ai-panel-header">
            <span>🤖</span> Gemini AI Assistant
            <span *ngIf="aiLoading()" class="spinner" style="width:15px;height:15px;margin-left:auto"></span>
          </div>

          <div *ngIf="validation() as v" class="flex items-center gap-16 mb-16">
            <div class="score-ring" [ngClass]="getScoreClass(v.qualityScore)">
              {{ v.qualityScore }}
            </div>
            <div>
              <div style="font-weight:600;font-size:14px">Quality Score</div>
              <div style="font-size:12px;color:var(--text-secondary)">{{ v.summary }}</div>
            </div>
          </div>

          <div *ngIf="!validation() && !aiLoading()"
               style="color:var(--text-secondary);font-size:13px">
            Start filling in the form — AI analysis appears automatically.
          </div>

          <div *ngIf="validation()?.issues?.length">
            <div class="field-group-label">Issues Found</div>
            <div *ngFor="let issue of validation()!.issues" class="gap-item" [ngClass]="issue.severity">
              <span class="gap-severity" [ngClass]="issue.severity">{{ issue.severity }}</span>
              <div style="font-size:13px">{{ issue.message }}</div>
            </div>
          </div>

          <div *ngIf="validation()?.suggestions?.acceptanceCriteria?.length" style="margin-top:12px">
            <div class="field-group-label">
              Suggested AC
              <button class="btn btn-secondary btn-sm" style="font-size:11px;margin-left:8px"
                      (click)="applyAcSuggestions()">Apply</button>
            </div>
            <div *ngFor="let ac of validation()!.suggestions.acceptanceCriteria"
                 class="ai-suggestion">{{ ac }}</div>
          </div>

          <div *ngIf="validation()?.suggestions?.estimationHint"
               class="alert alert-info" style="margin-top:12px;font-size:12px">
            💡 {{ validation()!.suggestions!.estimationHint }}
          </div>
        </div>

        <!-- Completion Checklist -->
        <div class="card">
          <div class="card-title">Completion Checklist</div>
          <div *ngFor="let item of checklist()" class="checklist-row">
            <span style="font-size:16px;flex-shrink:0">{{ item.done ? '✅' : '⬜' }}</span>
            <span style="font-size:13px;flex:1"
                  [style.color]="item.done ? 'var(--success)' : 'var(--text-secondary)'">
              {{ item.label }}
            </span>
            <span *ngIf="item.required && !item.done"
                  class="badge badge-danger" style="font-size:10px">REQUIRED</span>
            <span *ngIf="!item.required && !item.done"
                  class="badge badge-gray" style="font-size:10px">OPTIONAL</span>
          </div>
        </div>

      </div>
    </div>
  </div>
  `,
  styles: [`
    .section-head {
      font-size: 13px; font-weight: 700; text-transform: uppercase;
      letter-spacing: 0.06em; color: var(--text-muted);
      margin-bottom: 16px; padding-bottom: 8px;
      border-bottom: 1px solid var(--border);
    }
    .field-group-label {
      font-size: 11px; font-weight: 700; text-transform: uppercase;
      letter-spacing: 0.06em; color: var(--text-muted); margin-bottom: 8px;
    }
    .field-hint { display: flex; align-items: center; gap: 8px; margin-top: 6px; }

    /* Priority colours */
    .priority-blocker  { color: #dc2626; } .priority-critical { color: #ea580c; }
    .priority-major    { color: #d97706; } .priority-medium   { color: #16a34a; }
    .priority-minor    { color: #0ea5e9; } .priority-low      { color: #8b5cf6; }
    .priority-trivial  { color: #94a3b8; }

    /* Epic card */
    .epic-card {
      display: flex; align-items: center; gap: 10px;
      padding: 10px 14px; background: #f5f3ff;
      border: 1px solid #ddd6fe; border-radius: var(--radius); font-size: 13px;
    }
    .epic-key  { font-family: monospace; font-weight: 700; color: #5b21b6; }
    .epic-name { flex: 1; color: var(--text-primary); font-weight: 500; }
    .btn-icon  { background: none; border: none; cursor: pointer; color: var(--text-muted);
                 font-size: 18px; padding: 0 4px; }
    .btn-icon:hover { color: var(--danger); }

    /* Team grid */
    .team-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .team-option {
      display: inline-flex; align-items: center;
      padding: 5px 12px; border: 1px solid var(--border); border-radius: 9999px;
      font-size: 12px; font-weight: 500; cursor: pointer; transition: all 0.15s;
      color: var(--text-secondary);
    }
    .team-option:hover { border-color: var(--primary); color: var(--primary); }
    .team-option.selected {
      background: var(--primary-light); border-color: var(--primary);
      color: var(--primary);
    }

    /* Checklist */
    .checklist-row {
      display: flex; align-items: center; gap: 8px;
      padding: 7px 0; border-bottom: 1px solid var(--border);
    }
    .checklist-row:last-child { border-bottom: none; }
  `]
})
export class StoryFormComponent implements OnDestroy {

  projectKey = DEFAULT_PROJECT;

  // ── Static options ─────────────────────────────────────────────────────────
  priorities = [
    { value: 'Blocker',  label: 'Blocker',  icon: '🔴' },
    { value: 'Critical', label: 'Critical', icon: '🟠' },
    { value: 'Major',    label: 'Major',    icon: '🟡' },
    { value: 'Medium',   label: 'Medium',   icon: '🟢' },
    { value: 'Minor',    label: 'Minor',    icon: '🔵' },
    { value: 'Low',      label: 'Low',      icon: '⚪' },
    { value: 'Trivial',  label: 'Trivial',  icon: '⬜' },
  ];

  businessLines = [
    { value: 'Business',       label: '🏢 Business' },
    { value: 'GT',             label: '🌐 GT (Global Technology)' },
    { value: 'Platform',       label: '⚙️ Platform' },
    { value: 'Infrastructure', label: '🔧 Infrastructure' },
    { value: 'Data',           label: '📊 Data & Analytics' },
    { value: 'Security',       label: '🔒 Security' },
  ];

  teamOptions = [
    'Sigma Team', 'Invincible Team', 'Incredible Team',
    'Phoenix Team', 'Smelters Team'
  ];

  // ── Reactive state ─────────────────────────────────────────────────────────
  validation     = signal<ValidationResult | null>(null);
  aiLoading      = signal(false);
  saving         = signal(false);
  generatingAC   = signal(false);
  saveSuccess    = signal(false);
  saveError      = signal('');
  savedKey       = signal('');

  // Typeahead results
  sprintSuggestions     = signal<JiraSprint[]>([]);
  componentSuggestions  = signal<any[]>([]);
  versionSuggestions    = signal<any[]>([]);
  userSuggestions       = signal<JiraUser[]>([]);

  // Loading states
  sprintsLoading    = signal(false);
  componentsLoading = signal(false);
  versionsLoading   = signal(false);
  usersLoading      = signal(false);
  epicLoading       = signal(false);

  // Selected values from typeaheads
  selectedSprint    = signal<JiraSprint | null>(null);
  selectedComponents= signal<any[]>([]);
  selectedVersions  = signal<any[]>([]);
  selectedReporter  = signal<JiraUser | null>(null);
  selectedTeams     = signal<string[]>([]);
  selectedLabels    = signal<string[]>([]);
  resolvedEpic      = signal<JiraEpic | null>(null);
  epicNotFound      = signal(false);

  // ── Form ───────────────────────────────────────────────────────────────────
  form: FormGroup;

  private destroy$ = new Subject<void>();
  private validate$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private aiService: AiService,
    private jiraService: JiraService
  ) {
    this.form = this.fb.group({
      issueType:           ['Story', Validators.required],
      priority:            ['', Validators.required],
      businessLine:        ['', Validators.required],
      title:               ['', [Validators.required, Validators.minLength(5)]],
      description:         ['', [Validators.required, Validators.minLength(20)]],
      acceptanceCriteria:  ['', Validators.required],
      storyPoints:         [''],
      epicLink:            [''],
    });

    // Debounced AI validation on form change
    this.validate$.pipe(
      debounceTime(1200), distinctUntilChanged(), takeUntil(this.destroy$)
    ).subscribe(() => this.runValidation());

    this.form.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.form.value.title?.length > 3) this.validate$.next();
      });

    // Load initial sprint list
    this.loadSprints('');
  }

  get f() { return this.form.controls; }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && c?.touched);
  }

  // ── Typeahead event handlers ───────────────────────────────────────────────

  onSprintQuery(q: string): void {
    this.sprintsLoading.set(true);
    this.jiraService.searchSprints(this.projectKey, q).subscribe({
      next: r => { this.sprintSuggestions.set(r.data); this.sprintsLoading.set(false); },
      error: () => this.sprintsLoading.set(false)
    });
  }

  onSprintSelected(sprint: JiraSprint | null): void {
    this.selectedSprint.set(sprint);
  }

  private loadSprints(q: string): void { this.onSprintQuery(q); }

  onComponentQuery(q: string): void {
    this.componentsLoading.set(true);
    this.jiraService.searchComponents(this.projectKey, q).subscribe({
      next: r => { this.componentSuggestions.set(r.data); this.componentsLoading.set(false); },
      error: () => this.componentsLoading.set(false)
    });
  }

  onComponentsChanged(items: any[]): void { this.selectedComponents.set(items); }

  onVersionQuery(q: string): void {
    this.versionsLoading.set(true);
    this.jiraService.searchVersions(this.projectKey, q).subscribe({
      next: r => { this.versionSuggestions.set(r.data); this.versionsLoading.set(false); },
      error: () => this.versionsLoading.set(false)
    });
  }

  onVersionsChanged(items: any[]): void { this.selectedVersions.set(items); }

  onUserQuery(q: string): void {
    if (q.length < 2) { this.userSuggestions.set([]); return; }
    this.usersLoading.set(true);
    this.jiraService.searchUsers(q).subscribe({
      next: r => { this.userSuggestions.set(r.data); this.usersLoading.set(false); },
      error: () => this.usersLoading.set(false)
    });
  }

  onReporterSelected(user: JiraUser | null): void { this.selectedReporter.set(user); }

  onLabelsChanged(labels: string[]): void { this.selectedLabels.set(labels); }

  toggleTeam(team: string): void {
    const teams = this.selectedTeams();
    this.selectedTeams.set(
      teams.includes(team) ? teams.filter(t => t !== team) : [...teams, team]
    );
  }

  // ── Epic Link resolution ───────────────────────────────────────────────────

  onEpicInput(event: Event): void {
    // Clear resolved state when user edits the field
    this.resolvedEpic.set(null);
    this.epicNotFound.set(false);
    const val = (event.target as HTMLInputElement).value.toUpperCase();
    (event.target as HTMLInputElement).value = val;
    this.form.get('epicLink')?.setValue(val, { emitEvent: false });
  }

  resolveEpic(): void {
    const key = this.form.value.epicLink?.trim();
    if (!key || this.resolvedEpic()) return;
    this.epicLoading.set(true);
    this.epicNotFound.set(false);
    this.jiraService.getEpic(key).subscribe({
      next: r => {
        this.resolvedEpic.set(r.data);
        this.epicLoading.set(false);
      },
      error: () => {
        this.epicNotFound.set(true);
        this.epicLoading.set(false);
      }
    });
  }

  clearEpic(): void {
    this.resolvedEpic.set(null);
    this.epicNotFound.set(false);
    this.form.patchValue({ epicLink: '' });
  }

  // ── AI helpers ─────────────────────────────────────────────────────────────

  runValidation(): void {
    const v = this.form.value;
    if (!v.title) return;
    this.aiLoading.set(true);
    this.aiService.validate({
      title: v.title, description: v.description,
      acceptanceCriteria: v.acceptanceCriteria,
      issueType: v.issueType, storyPoints: v.storyPoints
    }).subscribe({
      next: r => { this.validation.set(r.data); this.aiLoading.set(false); },
      error: () => this.aiLoading.set(false)
    });
  }

  generateAC(): void {
    const v = this.form.value;
    if (!v.title) return;
    this.generatingAC.set(true);
    this.aiService.generateAC(v.title, v.description).subscribe({
      next: r => {
        this.form.patchValue({ acceptanceCriteria: r.data.join('\n\n') });
        this.generatingAC.set(false);
      },
      error: () => this.generatingAC.set(false)
    });
  }

  applyAcSuggestions(): void {
    const s = this.validation()?.suggestions?.acceptanceCriteria;
    if (s?.length) this.form.patchValue({ acceptanceCriteria: s.join('\n\n') });
  }

  // ── Submit ─────────────────────────────────────────────────────────────────

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.saveError.set('');

    const v = this.form.value;
    const sprint = this.selectedSprint();
    const reporter = this.selectedReporter();
    const epic = this.resolvedEpic();

    const payload = {
      projectKey:          this.projectKey,
      summary:             v.title,
      description:         v.description,
      acceptanceCriteria:  v.acceptanceCriteria,
      issueType:           v.issueType,
      priority:            v.priority,
      businessLine:        v.businessLine,
      storyPoints:         v.storyPoints ? +v.storyPoints : undefined,
      sprintId:            sprint?.id,
      sprintName:          sprint?.name,
      epicLink:            epic?.issueKey ?? v.epicLink,
      epicName:            epic?.name,
      reporterAccountId:   reporter?.accountId,
      reporterName:        reporter?.displayName,
      components:          this.selectedComponents().map(c => c.name),
      fixVersions:         this.selectedVersions().map(v2 => v2.name),
      labels:              this.selectedLabels(),
      teamNames:           this.selectedTeams(),
    };

    this.jiraService.createIssue(payload).subscribe({
      next: r => {
        this.savedKey.set(r.data.key);
        this.saving.set(false);
        this.saveSuccess.set(true);
        setTimeout(() => this.saveSuccess.set(false), 4000);
        this.resetForm();
      },
      error: err => {
        this.saveError.set(err.error?.message ?? 'Failed to create story. Please try again.');
        this.saving.set(false);
      }
    });
  }

  resetForm(): void {
    this.form.reset({ issueType: 'Story', priority: '', businessLine: '' });
    this.selectedSprint.set(null);
    this.selectedComponents.set([]);
    this.selectedVersions.set([]);
    this.selectedReporter.set(null);
    this.selectedTeams.set([]);
    this.selectedLabels.set([]);
    this.resolvedEpic.set(null);
    this.epicNotFound.set(false);
    this.validation.set(null);
    this.saveError.set('');
  }

  // ── Checklist ──────────────────────────────────────────────────────────────

  checklist() {
    const v = this.form.value;
    return [
      { label: 'Story title (5+ chars)',     done: (v.title?.length ?? 0) >= 5,       required: true  },
      { label: 'Description (20+ chars)',    done: (v.description?.length ?? 0) >= 20, required: true  },
      { label: 'Acceptance Criteria',        done: !!v.acceptanceCriteria,              required: true  },
      { label: 'Priority set',               done: !!v.priority,                        required: true  },
      { label: 'Business Line set',          done: !!v.businessLine,                    required: true  },
      { label: 'Story points estimated',     done: !!v.storyPoints,                     required: false },
      { label: 'Sprint assigned',            done: !!this.selectedSprint(),             required: false },
      { label: 'Epic link resolved',         done: !!this.resolvedEpic(),              required: false },
      { label: 'Component/s added',          done: this.selectedComponents().length > 0, required: false },
      { label: 'Fix version/s added',        done: this.selectedVersions().length > 0,  required: false },
      { label: 'Reporter assigned',          done: !!this.selectedReporter(),           required: false },
      { label: 'Team name/s selected',       done: this.selectedTeams().length > 0,     required: false },
      { label: 'AI score ≥ 60',             done: (this.validation()?.qualityScore ?? 0) >= 60, required: false },
    ];
  }

  // ── Display helpers ────────────────────────────────────────────────────────

  getScoreClass(score: number): string {
    return score >= 75 ? 'high' : score >= 50 ? 'mid' : 'low';
  }

  sprintBadge(state: string): string {
    return state === 'active' ? 'badge-success' : state === 'future' ? 'badge-info' : 'badge-gray';
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}
