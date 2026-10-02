import { Component, input, output } from '@angular/core';
import { Booking } from '../core/models';
import { LongDatePipe, PricePipe, TimeRangePipe } from '../shared/format-pipes';

@Component({
  selector: 'app-booking-card',
  imports: [LongDatePipe, PricePipe, TimeRangePipe],
  templateUrl: './booking-card.html',
  styleUrl: './booking-card.css',
})
export class BookingCard {
  readonly booking = input.required<Booking>();
  /** True while this booking is being cancelled. */
  readonly busy = input(false);

  readonly cancelRequested = output<Booking>();
}
