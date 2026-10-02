import { Pipe, PipeTransform } from '@angular/core';
import { formatLongDate, formatPrice, formatTimeRange } from './format';

// Thin template wrappers around the pure functions in format.ts (which hold the tested logic).

@Pipe({ name: 'price' })
export class PricePipe implements PipeTransform {
  transform(amount: number): string {
    return formatPrice(amount);
  }
}

@Pipe({ name: 'longDate' })
export class LongDatePipe implements PipeTransform {
  transform(iso: string): string {
    return formatLongDate(iso);
  }
}

@Pipe({ name: 'timeRange' })
export class TimeRangePipe implements PipeTransform {
  transform(startIso: string, endIso: string): string {
    return formatTimeRange(startIso, endIso);
  }
}
