import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Controls the session-expired modal popup.
 * Any component can inject this and read isExpired() to show the overlay.
 * The interceptor calls triggerExpiry() on every 401 response.
 */
@Injectable({ providedIn: 'root' })
export class SessionExpiredService {
  isExpired = signal(false);
  expiredAt = signal('');

  constructor(private router: Router) {}

  triggerExpiry(): void {
    if (this.isExpired()) return; // prevent duplicate triggers
    this.isExpired.set(true);
    this.expiredAt.set(new Date().toLocaleTimeString());
  }

  dismiss(): void {
    this.isExpired.set(false);
    this.router.navigate(['/login']);
  }
}
