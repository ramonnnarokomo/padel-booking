import { HttpErrorResponse } from '@angular/common/http';
import { NETWORK_ERROR_MESSAGE, toApiError } from './api-error';

describe('toApiError', () => {
  it('uses the ProblemDetail "detail" as message', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: {
        type: 'about:blank',
        title: 'Conflict',
        status: 409,
        detail: 'La pista ya está reservada en ese horario.',
        instance: '/api/bookings',
      },
    });

    expect(toApiError(error)).toEqual({
      status: 409,
      message: 'La pista ya está reservada en ese horario.',
      fieldErrors: [],
    });
  });

  it('includes validation field errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        title: 'Bad Request',
        status: 400,
        detail: 'Datos no válidos.',
        errors: { playerEmail: 'must be a well-formed email address' },
      },
    });

    expect(toApiError(error).fieldErrors).toEqual([
      { field: 'playerEmail', message: 'must be a well-formed email address' },
    ]);
  });

  it('explains how to fix it when the backend is unreachable', () => {
    const offline = new HttpErrorResponse({ status: 0 });
    const proxyDown = new HttpErrorResponse({ status: 502, error: '' });

    expect(toApiError(offline).message).toBe(NETWORK_ERROR_MESSAGE);
    expect(toApiError(proxyDown).message).toBe(NETWORK_ERROR_MESSAGE);
  });

  it('falls back to a generic message when the body is not a ProblemDetail', () => {
    const error = new HttpErrorResponse({ status: 500, error: 'boom' });

    expect(toApiError(error).message).toBe('Error inesperado (HTTP 500).');
    expect(toApiError(new Error('x')).message).toBe('Ha ocurrido un error inesperado.');
  });
});
