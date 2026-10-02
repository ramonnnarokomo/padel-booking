import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormField, FormRoot, email, form, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { ApiError, toApiError } from '../core/api-error';
import { Booking } from '../core/models';
import { PadelApi } from '../core/padel-api';
import { loadPlayer } from '../core/player-storage';
import { Toaster } from '../core/toaster';
import { ErrorAlert } from '../shared/error-alert';
import { formatLongDate, formatTime } from '../shared/format';
import { BookingCard } from './booking-card';

@Component({
  selector: 'app-my-bookings-page',
  imports: [BookingCard, ErrorAlert, FormField, FormRoot, RouterLink],
  templateUrl: './my-bookings-page.html',
  styleUrl: './my-bookings-page.css',
})
export class MyBookingsPage {
  private readonly api = inject(PadelApi);
  private readonly toaster = inject(Toaster);

  private readonly savedEmail = loadPlayer()?.email ?? '';

  private readonly search = signal({ email: this.savedEmail });
  protected readonly searchForm = form(
    this.search,
    (path) => {
      required(path.email, { message: 'Introduce tu email.' });
      email(path.email, { message: 'Introduce un email válido.' });
    },
    { submission: { action: async () => this.showBookingsOf(this.search().email.trim()) } },
  );

  /** Email whose bookings are listed. If one was saved, they are loaded straight away. */
  protected readonly email = signal(this.savedEmail);

  protected readonly bookings = rxResource({
    params: () => this.email() || undefined,
    stream: ({ params: email }) => this.api.getBookings(email),
  });

  protected readonly bookingsError = computed(() => {
    const error = this.bookings.error();
    return error ? toApiError(error) : null;
  });

  protected readonly cancellingId = signal<number | null>(null);
  protected readonly cancelError = signal<ApiError | null>(null);

  protected cancel(booking: Booking): void {
    const when = `${formatLongDate(booking.start).toLowerCase()} a las ${formatTime(booking.start)}`;
    if (!confirm(`¿Cancelar la reserva de ${booking.courtName} del ${when}?`)) {
      return;
    }

    this.cancellingId.set(booking.id);
    this.cancelError.set(null);
    this.api.cancelBooking(booking.id, this.email()).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.toaster.show('Reserva cancelada');
        this.bookings.reload();
      },
      error: (error: unknown) => {
        this.cancellingId.set(null);
        this.cancelError.set(toApiError(error));
      },
    });
  }

  private showBookingsOf(email: string): void {
    this.cancelError.set(null);
    if (email === this.email()) {
      this.bookings.reload(); // Same email: the resource would not refetch on its own.
    } else {
      this.email.set(email);
    }
  }
}
