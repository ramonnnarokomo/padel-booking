import { CourtSchedule, DaySchedule } from '../core/models';
import {
  buildTimeSlots,
  durationOptions,
  isDurationAvailable,
  isPastSlot,
  isPeakSlot,
  slotState,
  toMinutes,
  toTime,
} from './availability';

const court: CourtSchedule = {
  courtId: 1,
  courtName: 'Pista 1',
  indoor: true,
  pricePerHour: 18,
  bookedSlots: ['18:00', '18:30', '19:00'],
};

const day: DaySchedule = {
  date: '2026-10-05',
  openingTime: '09:00',
  closingTime: '23:00',
  slotMinutes: 30,
  courts: [court],
};

const available = (start: string) =>
  durationOptions(day, court, start)
    .filter((option) => option.available)
    .map((option) => option.minutes);

describe('availability', () => {
  it('converts between "HH:mm" and minutes', () => {
    expect(toMinutes('18:30')).toBe(1110);
    expect(toMinutes('09:00:00')).toBe(540);
    expect(toTime(1110)).toBe('18:30');
    expect(toTime(540)).toBe('09:00');
  });

  it('builds the 30-min slots between opening and closing time', () => {
    const slots = buildTimeSlots('09:00', '23:00', 30);

    expect(slots.length).toBe(28);
    expect(slots[0]).toBe('09:00');
    expect(slots[1]).toBe('09:30');
    expect(slots.at(-1)).toBe('22:30');
  });

  it('offers every duration when the court is free long enough', () => {
    expect(available('10:00')).toEqual([60, 90, 120]);
  });

  it('allows a booking that ends exactly when the next one starts', () => {
    expect(isDurationAvailable(day, court, '16:00', 120)).toBe(true);
    expect(available('16:30')).toEqual([60, 90]);
  });

  it('disables durations that overlap a booked slot', () => {
    expect(available('17:00')).toEqual([60]);
    expect(available('17:30')).toEqual([]);
  });

  it('disables durations that would end after closing time', () => {
    expect(available('21:00')).toEqual([60, 90, 120]);
    expect(available('21:30')).toEqual([60, 90]);
    expect(available('22:00')).toEqual([60]);
    expect(available('22:30')).toEqual([]);
  });

  it('accepts booked slots sent with seconds ("18:00:00")', () => {
    const withSeconds = { ...court, bookedSlots: ['18:00:00'] };
    expect(isDurationAvailable(day, withSeconds, '17:30', 60)).toBe(false);
  });

  it('detects past slots using local time', () => {
    const now = new Date(2026, 9, 5, 18, 10); // 5 Oct 2026, 18:10

    expect(isPastSlot('2026-10-05', '18:00', now)).toBe(true);
    expect(isPastSlot('2026-10-05', '18:30', now)).toBe(false);
    expect(isPastSlot('2026-10-04', '22:30', now)).toBe(true);
  });

  it('marks weekday slots from 18:00 as peak hours', () => {
    // 5 Oct 2026 is a Monday and 9 Oct a Friday.
    expect(isPeakSlot('2026-10-05', '17:30')).toBe(false);
    expect(isPeakSlot('2026-10-05', '18:00')).toBe(true);
    expect(isPeakSlot('2026-10-09', '22:30')).toBe(true);
  });

  it('has no peak hours at weekends', () => {
    expect(isPeakSlot('2026-10-10', '18:00')).toBe(false); // Saturday
    expect(isPeakSlot('2026-10-11', '21:00')).toBe(false); // Sunday
  });

  it('computes the state of each grid cell', () => {
    const morning = new Date(2026, 9, 5, 10, 15);

    expect(slotState(day, court, '10:00', morning)).toBe('past');
    expect(slotState(day, court, '10:30', morning)).toBe('free');
    expect(slotState(day, court, '18:30', morning)).toBe('booked');
    expect(slotState(day, court, '17:30', morning)).toBe('too-short');
    expect(slotState(day, court, '22:30', morning)).toBe('too-short');
  });
});
