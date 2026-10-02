import {
  Component,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  input,
  linkedSignal,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormField, FormRoot, email, form, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../core/api-error';
import { Booking, CourtSchedule, DaySchedule } from '../core/models';
import { PadelApi } from '../core/padel-api';
import { loadPlayer, savePlayer } from '../core/player-storage';
import { ErrorAlert } from '../shared/error-alert';
import { LongDatePipe, PricePipe } from '../shared/format-pipes';
import { durationOptions, toMinutes, toTime } from './availability';

/** Modal dialog to book the selected slot: duration, live price and player details. */
@Component({
  selector: 'app-booking-panel',
  imports: [ErrorAlert, FormField, FormRoot, LongDatePipe, PricePipe],
  templateUrl: './booking-panel.html',
  styleUrl: './booking-panel.css',
})
export class BookingPanel {
  private readonly api = inject(PadelApi);

  readonly day = input.required<DaySchedule>();
  readonly court = input.required<CourtSchedule>();
  readonly time = input.required<string>();

  readonly booked = output<Booking>();
  readonly closed = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly options = computed(() =>
    durationOptions(this.day(), this.court(), this.time()),
  );
  protected readonly someUnavailable = computed(() => this.options().some((o) => !o.available));

  /** Selected duration. Starts at (and resets to) the first option that fits. */
  protected readonly duration = linkedSignal(
    () => this.options().find((option) => option.available)?.minutes ?? null,
  );

  protected readonly start = computed(() => `${this.day().date}T${this.time()}`);
  protected readonly endTime = computed(() => {
    const duration = this.duration();
    return duration === null ? null : toTime(toMinutes(this.time()) + duration);
  });

  /** Asks the backend for the price every time the duration changes. */
  protected readonly quote = rxResource({
    params: () => {
      const durationMinutes = this.duration();
      return durationMinutes === null
        ? undefined
        : { courtId: this.court().courtId, start: this.start(), durationMinutes };
    },
    stream: ({ params }) => this.api.getQuote(params),
  });

  protected readonly quoteError = computed(() => {
    const error = this.quote.error();
    return error ? toApiError(error) : null;
  });

  private readonly savedPlayer = loadPlayer();
  private readonly player = signal({
    playerName: this.savedPlayer?.name ?? '',
    playerEmail: this.savedPlayer?.email ?? '',
  });

  protected readonly playerForm = form(
    this.player,
    (path) => {
      required(path.playerName, { message: 'Introduce tu nombre.' });
      required(path.playerEmail, { message: 'Introduce tu email.' });
      email(path.playerEmail, { message: 'Introduce un email válido.' });
    },
    // Runs on submit, only when the form is valid (wired through [formRoot]).
    { submission: { action: () => this.book() } },
  );

  protected readonly submitError = signal<ApiError | null>(null);

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }

  protected close(): void {
    // Fires the dialog's (close) event, which emits `closed`.
    this.dialog().nativeElement.close();
  }

  protected onDialogClick(event: MouseEvent): void {
    // A click on the <dialog> itself (not its content) is a click on the backdrop.
    if (event.target === this.dialog().nativeElement) {
      this.close();
    }
  }

  private async book(): Promise<void> {
    const durationMinutes = this.duration();
    if (durationMinutes === null) {
      return;
    }
    const playerName = this.player().playerName.trim();
    const playerEmail = this.player().playerEmail.trim();

    this.submitError.set(null);
    try {
      const booking = await firstValueFrom(
        this.api.createBooking({
          courtId: this.court().courtId,
          playerName,
          playerEmail,
          start: this.start(),
          durationMinutes,
        }),
      );
      savePlayer({ name: playerName, email: playerEmail });
      this.booked.emit(booking);
    } catch (error) {
      this.submitError.set(toApiError(error));
    }
  }
}
