import { Injectable, signal } from '@angular/core';

/** Short confirmation messages shown at the bottom of the screen (rendered by App). */
@Injectable({ providedIn: 'root' })
export class Toaster {
  private readonly _message = signal<string | null>(null);
  private timeoutId?: ReturnType<typeof setTimeout>;

  readonly message = this._message.asReadonly();

  show(message: string, durationMs = 4000): void {
    clearTimeout(this.timeoutId);
    this._message.set(message);
    this.timeoutId = setTimeout(() => this._message.set(null), durationMs);
  }

  dismiss(): void {
    clearTimeout(this.timeoutId);
    this._message.set(null);
  }
}
