import { addDays, parseIsoDate, toIsoDate, todayIso } from './dates';
import { formatLongDate, formatPrice, formatTime, formatTimeRange } from './format';

// Intl puts a non-breaking space (U+00A0) between the amount and the euro sign.
const NBSP = ' ';

describe('formatPrice', () => {
  it('hides decimals for whole amounts', () => {
    expect(formatPrice(18)).toBe(`18${NBSP}€`);
  });

  it('uses a comma and two decimals otherwise', () => {
    expect(formatPrice(33.75)).toBe(`33,75${NBSP}€`);
    expect(formatPrice(22.5)).toBe(`22,50${NBSP}€`);
  });
});

describe('date and time formatting', () => {
  it('formats a day as weekday + date in Spanish, capitalised', () => {
    expect(formatLongDate('2026-10-05')).toBe('Lunes, 5 de octubre');
  });

  it('accepts a full date-time as well', () => {
    expect(formatLongDate('2026-10-10T18:00:00')).toBe('Sábado, 10 de octubre');
  });

  it('extracts the time and builds ranges', () => {
    expect(formatTime('2026-10-05T18:00:00')).toBe('18:00');
    expect(formatTimeRange('2026-10-05T18:00:00', '2026-10-05T19:30:00')).toBe('18:00 – 19:30');
  });
});

describe('date helpers', () => {
  it('converts between Date and "YYYY-MM-DD" in local time', () => {
    expect(toIsoDate(new Date(2026, 0, 7))).toBe('2026-01-07');
    expect(parseIsoDate('2026-10-05')).toEqual(new Date(2026, 9, 5));
    expect(todayIso(new Date(2026, 9, 2, 23, 59))).toBe('2026-10-02');
  });

  it('adds days across month and year boundaries', () => {
    expect(addDays('2026-10-31', 1)).toBe('2026-11-01');
    expect(addDays('2026-12-25', 14)).toBe('2027-01-08');
    expect(addDays('2026-10-01', -1)).toBe('2026-09-30');
  });

  it('is not affected by daylight saving changes', () => {
    // Spain switches to winter time on 25 Oct 2026.
    expect(addDays('2026-10-24', 1)).toBe('2026-10-25');
    expect(addDays('2026-10-25', 1)).toBe('2026-10-26');
  });
});
