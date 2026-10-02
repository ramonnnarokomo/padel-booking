import { Component, input } from '@angular/core';
import { ApiError } from '../core/api-error';

const FIELD_LABELS: Record<string, string> = {
  courtId: 'Pista',
  playerName: 'Nombre',
  playerEmail: 'Email',
  start: 'Hora de inicio',
  durationMinutes: 'Duración',
  email: 'Email',
};

/** Shows the `detail` of a ProblemDetail plus its field errors, if any. */
@Component({
  selector: 'app-error-alert',
  templateUrl: './error-alert.html',
  styleUrl: './error-alert.css',
})
export class ErrorAlert {
  readonly error = input.required<ApiError>();

  protected label(field: string): string {
    return FIELD_LABELS[field] ?? field;
  }
}
