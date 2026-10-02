// Shapes returned and accepted by the Spring Boot API (base path /api).
// Dates travel as ISO strings: "2026-10-05" for days, "18:00" for times
// and "2026-10-05T18:00:00" for date-times (local club time, no offset).

export interface Court {
  id: number;
  name: string;
  indoor: boolean;
  pricePerHour: number;
}

export interface CourtSchedule {
  courtId: number;
  courtName: string;
  indoor: boolean;
  pricePerHour: number;
  /** Start times ("18:00", "18:30"...) of the 30-min slots already taken that day. */
  bookedSlots: string[];
}

export interface DaySchedule {
  date: string;
  openingTime: string;
  closingTime: string;
  slotMinutes: number;
  courts: CourtSchedule[];
}

export interface QuoteQuery {
  courtId: number;
  start: string;
  durationMinutes: number;
}

export interface Quote {
  courtId: number;
  start: string;
  end: string;
  durationMinutes: number;
  price: number;
  peak: boolean;
}

export interface BookingRequest {
  courtId: number;
  playerName: string;
  playerEmail: string;
  start: string;
  durationMinutes: number;
}

export type BookingStatus = 'CONFIRMED' | 'CANCELLED';

export interface Booking {
  id: number;
  courtId: number;
  courtName: string;
  playerName: string;
  playerEmail: string;
  start: string;
  end: string;
  durationMinutes: number;
  price: number;
  peak: boolean;
  status: BookingStatus;
  cancellable: boolean;
}

/** RFC 9457 error body. `errors` is only present on 400 validation errors. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}
