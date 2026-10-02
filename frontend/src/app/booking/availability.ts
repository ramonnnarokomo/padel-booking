import { CourtSchedule, DaySchedule } from '../core/models';

// Pure functions (no Angular) that decide what can be booked. The backend validates
// the same rules again; this is only so the UI never offers an impossible option.

export const DURATIONS = [60, 90, 120] as const;
export const BOOKING_WINDOW_DAYS = 14;

export type SlotState = 'free' | 'booked' | 'past' | 'too-short';

export interface DurationOption {
  minutes: number;
  available: boolean;
}

/** "18:30" (or "18:30:00") → 1110 minutes since midnight. */
export function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

/** 1110 → "18:30". */
export function toTime(totalMinutes: number): string {
  const hours = String(Math.floor(totalMinutes / 60)).padStart(2, '0');
  const minutes = String(totalMinutes % 60).padStart(2, '0');
  return `${hours}:${minutes}`;
}

/** Start time of every slot of the day: "09:00", "09:30" ... "22:30". */
export function buildTimeSlots(
  openingTime: string,
  closingTime: string,
  slotMinutes: number,
): string[] {
  const slots: string[] = [];
  for (let t = toMinutes(openingTime); t < toMinutes(closingTime); t += slotMinutes) {
    slots.push(toTime(t));
  }
  return slots;
}

/** True when [start, start + duration) ends before closing and touches no booked slot. */
export function isDurationAvailable(
  day: DaySchedule,
  court: CourtSchedule,
  start: string,
  durationMinutes: number,
): boolean {
  const startMinutes = toMinutes(start);
  const endMinutes = startMinutes + durationMinutes;
  if (endMinutes > toMinutes(day.closingTime)) {
    return false;
  }

  const booked = new Set(court.bookedSlots.map((slot) => toTime(toMinutes(slot))));
  for (let t = startMinutes; t < endMinutes; t += day.slotMinutes) {
    if (booked.has(toTime(t))) {
      return false;
    }
  }
  return true;
}

export function durationOptions(
  day: DaySchedule,
  court: CourtSchedule,
  start: string,
): DurationOption[] {
  return DURATIONS.map((minutes) => ({
    minutes,
    available: isDurationAvailable(day, court, start, minutes),
  }));
}

export function isPastSlot(date: string, time: string, now: Date): boolean {
  // "2026-10-05T18:00" without offset is parsed as local time.
  return new Date(`${date}T${toTime(toMinutes(time))}`) < now;
}

/**
 * What a grid cell shows. A free slot where not even the shortest duration fits
 * (e.g. 22:30, or a 30-min gap between two bookings) is "too-short".
 */
export function slotState(
  day: DaySchedule,
  court: CourtSchedule,
  time: string,
  now: Date,
): SlotState {
  if (isPastSlot(day.date, time, now)) {
    return 'past';
  }
  if (court.bookedSlots.some((slot) => toMinutes(slot) === toMinutes(time))) {
    return 'booked';
  }
  return isDurationAvailable(day, court, time, DURATIONS[0]) ? 'free' : 'too-short';
}
