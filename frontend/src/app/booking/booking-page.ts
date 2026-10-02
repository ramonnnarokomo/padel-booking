import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { toApiError } from '../core/api-error';
import { CourtSchedule, DaySchedule } from '../core/models';
import { PadelApi } from '../core/padel-api';
import { Toaster } from '../core/toaster';
import { addDays, todayIso } from '../shared/dates';
import { ErrorAlert } from '../shared/error-alert';
import { LongDatePipe, PricePipe } from '../shared/format-pipes';
import { BOOKING_WINDOW_DAYS, SlotState, buildTimeSlots, slotState } from './availability';
import { BookingPanel } from './booking-panel';

interface GridRow {
  time: string;
  cells: { court: CourtSchedule; state: SlotState }[];
}

interface SelectedSlot {
  day: DaySchedule;
  court: CourtSchedule;
  time: string;
}

@Component({
  selector: 'app-booking-page',
  imports: [BookingPanel, ErrorAlert, LongDatePipe, PricePipe],
  templateUrl: './booking-page.html',
  styleUrl: './booking-page.css',
})
export class BookingPage {
  private readonly api = inject(PadelApi);
  private readonly toaster = inject(Toaster);

  protected readonly today = todayIso();
  protected readonly lastDay = addDays(this.today, BOOKING_WINDOW_DAYS);
  protected readonly date = signal(this.today);

  /** Updated every minute so slots become "past" while the page stays open. */
  private readonly now = signal(new Date());

  /** Reloads automatically every time `date` changes. */
  protected readonly schedule = rxResource({
    params: () => this.date(),
    stream: ({ params: date }) => this.api.getSchedule(date),
  });

  protected readonly day = computed(() =>
    this.schedule.hasValue() ? this.schedule.value() : null,
  );

  protected readonly scheduleError = computed(() => {
    const error = this.schedule.error();
    return error ? toApiError(error) : null;
  });

  protected readonly rows = computed<GridRow[]>(() => {
    const day = this.day();
    if (!day) {
      return [];
    }
    const now = this.now();
    return buildTimeSlots(day.openingTime, day.closingTime, day.slotMinutes).map((time) => ({
      time,
      cells: day.courts.map((court) => ({ court, state: slotState(day, court, time, now) })),
    }));
  });

  protected readonly selected = signal<SelectedSlot | null>(null);

  constructor() {
    const timer = setInterval(() => this.now.set(new Date()), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected goTo(date: string): void {
    // Ignores empty or out-of-range values typed into the date input.
    if (date >= this.today && date <= this.lastDay) {
      this.date.set(date);
    }
  }

  protected moveDays(days: number): void {
    this.goTo(addDays(this.date(), days));
  }

  protected onDateChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.goTo(input.value);
    input.value = this.date(); // Puts back the current day if the value was rejected.
  }

  protected select(day: DaySchedule, court: CourtSchedule, time: string): void {
    this.selected.set({ day, court, time });
  }

  protected onBooked(): void {
    this.selected.set(null);
    this.toaster.show('Reserva confirmada');
    this.schedule.reload();
  }
}
