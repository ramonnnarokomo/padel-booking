import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Booking, BookingRequest, Court, DaySchedule, Quote, QuoteQuery } from './models';

/** Every call to the backend goes through this service. */
@Injectable({ providedIn: 'root' })
export class PadelApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  getCourts(): Observable<Court[]> {
    return this.http.get<Court[]>(`${this.baseUrl}/courts`);
  }

  getSchedule(date: string): Observable<DaySchedule> {
    return this.http.get<DaySchedule>(`${this.baseUrl}/schedule`, { params: { date } });
  }

  getQuote(query: QuoteQuery): Observable<Quote> {
    return this.http.get<Quote>(`${this.baseUrl}/quote`, { params: { ...query } });
  }

  createBooking(request: BookingRequest): Observable<Booking> {
    return this.http.post<Booking>(`${this.baseUrl}/bookings`, request);
  }

  getBookings(email: string): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.baseUrl}/bookings`, { params: { email } });
  }

  cancelBooking(id: number, email: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/bookings/${id}`, { params: { email } });
  }
}
