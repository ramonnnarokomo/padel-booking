import { parseIsoDate } from './dates';

const LOCALE = 'es-ES';

const longDateFormat = new Intl.DateTimeFormat(LOCALE, {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
});

/** 18 → "18 €", 33.75 → "33,75 €" (decimals only when needed). */
export function formatPrice(amount: number): string {
  return new Intl.NumberFormat(LOCALE, {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: Number.isInteger(amount) ? 0 : 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

/** "2026-10-05" or "2026-10-05T18:00:00" → "Lunes, 5 de octubre". */
export function formatLongDate(iso: string): string {
  const text = longDateFormat.format(parseIsoDate(iso));
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** "2026-10-05T18:00:00" → "18:00". */
export function formatTime(isoDateTime: string): string {
  return isoDateTime.slice(11, 16);
}

/** Two date-times → "18:00 – 19:30". */
export function formatTimeRange(startIso: string, endIso: string): string {
  return `${formatTime(startIso)} – ${formatTime(endIso)}`;
}
