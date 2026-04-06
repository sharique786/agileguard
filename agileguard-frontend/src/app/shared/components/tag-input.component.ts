import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

/**
 * Simple free-text tag input.
 * User types a label and presses Enter or comma to add it as a chip tag.
 * Used for the "Label" field in the Story Editor.
 */
@Component({
  selector: 'app-tag-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="tag-input-root">
      <label *ngIf="label" class="form-label">{{ label }}</label>
      <div class="tag-input-wrap" (click)="inputEl.focus()">
        <span *ngFor="let tag of tags" class="tag">
          {{ tag }}
          <button type="button" class="tag-remove" (click)="remove(tag)">×</button>
        </span>
        <input #inputEl
               class="tag-text-input"
               [placeholder]="tags.length === 0 ? placeholder : ''"
               [(ngModel)]="current"
               (keydown)="onKey($event)"
               (blur)="addCurrent()">
      </div>
      <div class="form-error" *ngIf="showError">{{ errorMessage }}</div>
      <div style="font-size:11px;color:var(--text-muted);margin-top:3px">
        Press <kbd>Enter</kbd> or <kbd>,</kbd> to add a tag
      </div>
    </div>
  `,
  styles: [`
    .tag-input-wrap {
      display: flex; flex-wrap: wrap; gap: 6px; align-items: center;
      min-height: 38px; padding: 5px 10px;
      border: 1px solid var(--border); border-radius: var(--radius);
      cursor: text; transition: border-color 0.15s;
    }
    .tag-input-wrap:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-light); }
    .tag {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 2px 8px; background: var(--primary-light); color: var(--primary);
      border-radius: 9999px; font-size: 12px; font-weight: 500;
    }
    .tag-remove {
      background: none; border: none; cursor: pointer; color: var(--primary);
      font-size: 16px; line-height: 1; padding: 0;
    }
    .tag-remove:hover { color: var(--danger); }
    .tag-text-input {
      border: none; outline: none; font-size: 13px; font-family: inherit;
      background: transparent; flex: 1; min-width: 120px;
    }
  `]
})
export class TagInputComponent {
  @Input() label       = '';
  @Input() placeholder = 'Add a label…';
  @Input() showError   = false;
  @Input() errorMessage= '';
  @Input() tags: string[] = [];

  @Output() tagsChange = new EventEmitter<string[]>();

  current = '';

  onKey(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.addCurrent();
    } else if (event.key === 'Backspace' && !this.current && this.tags.length > 0) {
      this.remove(this.tags[this.tags.length - 1]);
    }
  }

  addCurrent(): void {
    const val = this.current.replace(/,/g, '').trim();
    if (val && !this.tags.includes(val)) {
      this.tags = [...this.tags, val];
      this.tagsChange.emit(this.tags);
    }
    this.current = '';
  }

  remove(tag: string): void {
    this.tags = this.tags.filter(t => t !== tag);
    this.tagsChange.emit(this.tags);
  }
}
