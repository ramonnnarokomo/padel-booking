import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from './models';

export const NETWORK_ERROR_MESSAGE =
  'No se puede conectar con la API. ¿Está arrancado el backend en el puerto 8080?';

export interface FieldError {
  field: string;
  message: string;
}

/** What the UI needs to show for a failed request. */
export interface ApiError {
  status: number;
  message: string;
  fieldErrors: FieldError[];
}

// With `ng serve` the dev-server proxy answers 502 (empty body) when the backend is down.
const GATEWAY_STATUSES = [502, 503, 504];

export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: 0, message: 'Ha ocurrido un error inesperado.', fieldErrors: [] };
  }

  const problem = isProblemDetail(error.error) ? error.error : null;

  if (error.status === 0 || (!problem && GATEWAY_STATUSES.includes(error.status))) {
    return { status: error.status, message: NETWORK_ERROR_MESSAGE, fieldErrors: [] };
  }

  const fieldErrors = Object.entries(problem?.errors ?? {}).map(([field, message]) => ({
    field,
    message,
  }));

  return {
    status: error.status,
    message: problem?.detail ?? problem?.title ?? `Error inesperado (HTTP ${error.status}).`,
    fieldErrors,
  };
}

function isProblemDetail(body: unknown): body is ProblemDetail {
  return typeof body === 'object' && body !== null && ('detail' in body || 'title' in body);
}
