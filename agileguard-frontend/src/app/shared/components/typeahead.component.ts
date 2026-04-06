import {
  Component, Input, Output, EventEmitter, signal, OnDestroy,
  HostListener, ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

/**
 * Generic reusable typeahead (autocomplete) component.
 *
 * Supports two modes:
 *   single  — user picks exactly one item; the input is replaced by the selection.
 *   multi   — user picks multiple items shown as removable tag chips below the input.
 *
 * Usage example (Sprint field):
 *   <app-typeahead
 *     label="Sprint"
 *     [required]="true"
 *     placeholder="Search sprints…"
 *     displayKey="name"
 *     [items]="sprintSuggestions()"
 *     [loading]="sprintsLoading()"
 *     (queryChange)="onSprintQuery($event)"
 *     (selected)="onSprintSelected($event)"
 *   />
 *
 * Usage example (Components multi-select):
 *   <app-typeahead
 *     label="Component/s"
 *     mode="multi"
 *     displayKey="name"
 *     [items]="componentSuggestions()"
 *     [loading]="componentsLoading()"
 *     (queryChange)="onComponentQuery($event)"
 *     (multiSelected)="onComponentsChanged($event)"
 *   />
 */
@Component({
  selector: 'app-typeahead',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ta-root" [class.ta-error]="showError">

      <!-- Label row -->
      <label *ngIf="label" class="form-label" [class.required]="required">
        {{ label }}
        <span *ngIf="hint" class="text-muted text-xs" style="font-weight:400"> — {{ hint }}</span>
      </label>

      <!-- Selected tags (multi-mode) -->
      <div *ngIf="mode === 'multi' && selectedItems.length > 0" class="tag-row">
        <span *ngFor="let item of selectedItems" class="tag">
          {{ getLabel(item) }}
          <button type="button" class="tag-remove" (click)="removeItem(item)">×</button>
        </span>
      </div>

      <!-- Input + dropdown -->
      <div class="ta-input-wrap">
        <input
          class="form-control ta-input"
          [class.error]="showError"
          [placeholder]="placeholder"
          [(ngModel)]="inputValue"
          (ngModelChange)="onInputChange($event)"
          (focus)="onFocus()"
          (blur)="onBlur()"
          [readOnly]="mode === 'single' && !!singleSelected"
          autocomplete="off">

        <!-- Clear button (single mode) -->
        <button *ngIf="mode === 'single' && singleSelected"
                type="button" class="ta-clear" (click)="clearSingle()">×</button>

        <!-- Loading spinner -->
        <span *ngIf="loading" class="ta-spinner">
          <span class="spinner" style="width:14px;height:14px"></span>
        </span>

        <!-- Dropdown -->
        <div class="ta-dropdown" *ngIf="open && items.length > 0">
          <div *ngFor="let item of items"
               class="ta-option"
               (mousedown)="selectItem(item)">
            <span class="ta-option-label">{{ getLabel(item) }}</span>
            <span *ngIf="getSubLabel(item)" class="ta-option-sub">{{ getSubLabel(item) }}</span>
            <span *ngIf="isSelected(item)" class="ta-check">✓</span>
          </div>
        </div>

        <!-- No results -->
        <div class="ta-dropdown ta-no-results"
             *ngIf="open && !loading && items.length === 0 && inputValue.length > 0">
          No results for "{{ inputValue }}"
        </div>
      </div>

      <!-- Error message -->
      <div class="form-error" *ngIf="showError">{{ errorMessage }}</div>
    </div>
  `,
  styles: [`
    .ta-root { position: relative; }
    .ta-input-wrap { position: relative; }

    .ta-input { padding-right: 36px; }

    .ta-clear, .ta-spinner {
      position: absolute; right: 10px; top: 50%; transform: translateY(-50%);
    }
    .ta-clear {
      background: none; border: none; cursor: pointer; color: var(--text-muted);
      font-size: 18px; line-height: 1; padding: 0;
    }
    .ta-clear:hover { color: var(--danger); }

    .ta-dropdown {
      position: absolute; top: calc(100% + 4px); left: 0; right: 0; z-index: 9999;
      background: var(--surface); border: 1px solid var(--border);
      border-radius: var(--radius-lg); box-shadow: 0 8px 24px rgba(0,0,0,0.12);
      max-height: 240px; overflow-y: auto;
    }
    .ta-option {
      display: flex; align-items: center; gap: 8px;
      padding: 9px 14px; cursor: pointer; font-size: 13px;
      transition: background 0.1s;
    }
    .ta-option:hover { background: var(--bg); }
    .ta-option-label { flex: 1; color: var(--text-primary); font-weight: 500; }
    .ta-option-sub { font-size: 11px; color: var(--text-muted); }
    .ta-check { color: var(--success); font-weight: 700; margin-left: auto; }
    .ta-no-results { padding: 12px 14px; color: var(--text-muted); font-size: 13px; }

    /* Tags */
    .tag-row { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 6px; }
    .tag {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 3px 8px; background: var(--primary-light); color: var(--primary);
      border-radius: 9999px; font-size: 12px; font-weight: 500;
    }
    .tag-remove {
      background: none; border: none; cursor: pointer; color: var(--primary);
      font-size: 16px; line-height: 1; padding: 0; margin-left: 2px;
    }
    .tag-remove:hover { color: var(--danger); }
  `]
})
export class TypeaheadComponent implements OnDestroy {
  @Input() label       = '';
  @Input() hint        = '';
  @Input() placeholder = 'Type to search…';
  @Input() mode: 'single' | 'multi' = 'single';
  @Input() displayKey  = 'name';     // primary display field
  @Input() subKey      = '';         // optional secondary field (e.g., email under name)
  @Input() valueKey    = '';         // if set, emits item[valueKey] instead of whole object
  @Input() required    = false;
  @Input() showError   = false;
  @Input() errorMessage= 'This field is required';
  @Input() items: any[]= [];
  @Input() loading     = false;

  @Output() queryChange  = new EventEmitter<string>();   // fires on each debounced keystroke
  @Output() selected     = new EventEmitter<any>();      // single mode: emits selected item
  @Output() multiSelected= new EventEmitter<any[]>();    // multi mode: emits full array

  inputValue    = '';
  open          = false;
  singleSelected: any = null;
  selectedItems: any[]= [];

  private query$   = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(private elRef: ElementRef) {
    this.query$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(q => this.queryChange.emit(q));
  }

  onInputChange(val: string): void {
    this.open = true;
    this.query$.next(val);
  }

  onFocus(): void {
    if (!(this.mode === 'single' && this.singleSelected)) {
      this.open = true;
      if (!this.inputValue) this.query$.next('');
    }
  }

  onBlur(): void {
    // Small delay so mousedown on option fires first
    setTimeout(() => this.open = false, 180);
  }

  selectItem(item: any): void {
    if (this.mode === 'single') {
      this.singleSelected = item;
      this.inputValue = this.getLabel(item);
      this.open = false;
      this.selected.emit(this.valueKey ? item[this.valueKey] : item);
    } else {
      const already = this.selectedItems.find(s => this.getLabel(s) === this.getLabel(item));
      if (!already) {
        this.selectedItems = [...this.selectedItems, item];
        this.multiSelected.emit(this.selectedItems);
      }
      this.inputValue = '';
      this.queryChange.emit('');
    }
  }

  removeItem(item: any): void {
    this.selectedItems = this.selectedItems.filter(s => this.getLabel(s) !== this.getLabel(item));
    this.multiSelected.emit(this.selectedItems);
  }

  clearSingle(): void {
    this.singleSelected = null;
    this.inputValue = '';
    this.selected.emit(null);
    this.queryChange.emit('');
  }

  isSelected(item: any): boolean {
    if (this.mode === 'multi') {
      return this.selectedItems.some(s => this.getLabel(s) === this.getLabel(item));
    }
    return this.singleSelected && this.getLabel(this.singleSelected) === this.getLabel(item);
  }

  getLabel(item: any): string {
    if (!item) return '';
    return typeof item === 'string' ? item : (item[this.displayKey] ?? String(item));
  }

  getSubLabel(item: any): string {
    if (!this.subKey || !item) return '';
    return item[this.subKey] ?? '';
  }

  /** Programmatically set the value from a parent component. */
  setValue(item: any): void {
    if (this.mode === 'single') {
      this.singleSelected = item;
      this.inputValue = item ? this.getLabel(item) : '';
    } else if (Array.isArray(item)) {
      this.selectedItems = item;
    }
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}
