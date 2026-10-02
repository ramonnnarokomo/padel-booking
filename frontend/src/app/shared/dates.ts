// Small helpers to work with "YYYY-MM-DD" strings in the browser's local time zone.
// Using local Date getters (not toISOString, which is UTC) avoids off-by-one days.

export function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Accepts "2026-10-05" or a date-time like "2026-10-05T18:00:00" (only the date part is used). */
export function parseIsoDate(iso: string): Date {
  const [year, month, day] = iso.slice(0, 10).split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function addDays(iso: string, days: number): string {
  const date = parseIsoDate(iso);
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
}

export function todayIso(now = new Date()): string {
  return toIsoDate(now);
}
