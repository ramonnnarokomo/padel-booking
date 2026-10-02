import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Booking, CourtSchedule, DaySchedule, Quote } from '../core/models';
import { loadPlayer } from '../core/player-storage';
import { BookingPanel } from './booking-panel';

const court: CourtSchedule = {
  courtId: 1,
  courtName: 'Pista 1',
  indoor: true,
  pricePerHour: 18,
  bookedSlots: ['19:30'],
};

const day: DaySchedule = {
  date: '2026-10-05',
  openingTime: '09:00',
  closingTime: '23:00',
  slotMinutes: 30,
  courts: [court],
};

const quote: Quote = {
  courtId: 1,
  start: '2026-10-05T18:00:00',
  end: '2026-10-05T19:00:00',
  durationMinutes: 60,
  price: 22.5,
  peak: true,
};

const booking: Booking = {
  id: 10,
  courtId: 1,
  courtName: 'Pista 1',
  playerName: 'Ramón',
  playerEmail: 'ramon@example.com',
  start: '2026-10-05T18:00:00',
  end: '2026-10-05T19:00:00',
  durationMinutes: 60,
  price: 22.5,
  peak: true,
  status: 'CONFIRMED',
  cancellable: true,
};

describe('BookingPanel', () => {
  let fixture: ComponentFixture<BookingPanel>;
  let httpTesting: HttpTestingController;
  let element: HTMLElement;

  beforeAll(() => {
    // jsdom does not implement the <dialog> methods yet.
    HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
      this.open = true;
    };
    HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
      this.open = false;
      this.dispatchEvent(new Event('close'));
    };
  });

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(BookingPanel);
    fixture.componentRef.setInput('day', day);
    fixture.componentRef.setInput('court', court);
    fixture.componentRef.setInput('time', '18:00');
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => httpTesting.verify());

  function flushQuote(): void {
    httpTesting.expectOne((req) => req.url === '/api/quote').flush(quote);
  }

  function type(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submitForm(): void {
    element.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    fixture.detectChanges();
  }

  it('disables durations that overlap another booking', () => {
    flushQuote();

    // 18:00 + 120 min would reach the booking at 19:30.
    const radios = [...element.querySelectorAll<HTMLInputElement>('input[type=radio]')];
    expect(radios.map((radio) => radio.disabled)).toEqual([false, false, true]);
    expect(radios[0].checked).toBe(true);
  });

  it('shows the live price and the peak chip', async () => {
    const request = httpTesting.expectOne((req) => req.url === '/api/quote');
    expect(request.request.params.get('courtId')).toBe('1');
    expect(request.request.params.get('start')).toBe('2026-10-05T18:00');
    expect(request.request.params.get('durationMinutes')).toBe('60');

    request.flush(quote);
    await fixture.whenStable();

    expect(element.querySelector('.price')?.textContent).toContain('22,50');
    expect(element.querySelector('.chip-peak')?.textContent).toContain('Hora punta +25%');
  });

  it('asks for a new quote when the duration changes', async () => {
    flushQuote();

    element.querySelectorAll<HTMLInputElement>('input[type=radio]')[1].click();
    fixture.detectChanges();

    const request = httpTesting.expectOne((req) => req.url === '/api/quote');
    expect(request.request.params.get('durationMinutes')).toBe('90');
    request.flush({ ...quote, durationMinutes: 90, price: 33.75 });
    await fixture.whenStable();

    expect(element.querySelector('.price')?.textContent).toContain('33,75');
  });

  it('shows validation messages and does not send an invalid form', async () => {
    flushQuote();

    submitForm();
    await fixture.whenStable();

    expect(element.textContent).toContain('Introduce tu nombre.');
    expect(element.textContent).toContain('Introduce tu email.');
    httpTesting.expectNone({ method: 'POST' });
  });

  it('sends the booking, emits it and remembers the player', async () => {
    flushQuote();
    const booked = vi.fn();
    fixture.componentInstance.booked.subscribe(booked);

    type('#player-name', 'Ramón');
    type('#player-email', 'ramon@example.com');
    submitForm();

    const request = httpTesting.expectOne({ method: 'POST', url: '/api/bookings' });
    expect(request.request.body).toEqual({
      courtId: 1,
      playerName: 'Ramón',
      playerEmail: 'ramon@example.com',
      start: '2026-10-05T18:00',
      durationMinutes: 60,
    });
    request.flush(booking, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(booked).toHaveBeenCalledWith(booking);
    expect(loadPlayer()).toEqual({ name: 'Ramón', email: 'ramon@example.com' });
  });

  it('shows the problem detail when the API rejects the booking', async () => {
    flushQuote();
    type('#player-name', 'Ramón');
    type('#player-email', 'ramon@example.com');
    submitForm();

    httpTesting.expectOne({ method: 'POST', url: '/api/bookings' }).flush(
      {
        type: 'about:blank',
        title: 'Conflict',
        status: 409,
        detail: 'La pista ya está reservada en ese horario.',
        instance: '/api/bookings',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(element.querySelector('[role=alert]')?.textContent).toContain(
      'La pista ya está reservada en ese horario.',
    );
  });
});
