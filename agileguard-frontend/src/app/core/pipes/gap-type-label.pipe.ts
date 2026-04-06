import { Pipe, PipeTransform } from '@angular/core';

/**
 * Transforms a snake_case GapType enum value into a readable label.
 * Example: "MISSING_ACCEPTANCE_CRITERIA" → "Missing Acceptance Criteria"
 */
@Pipe({ name: 'gapTypeLabel', standalone: true })
export class GapTypeLabelPipe implements PipeTransform {
  transform(value: string | undefined | null): string {
    if (!value) return '';
    return value
      .split('_')
      .map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
      .join(' ');
  }
}
