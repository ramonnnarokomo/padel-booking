import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Reservar pista · PadelBook',
    loadComponent: () => import('./booking/booking-page').then((m) => m.BookingPage),
  },
  {
    path: 'mis-reservas',
    title: 'Mis reservas · PadelBook',
    loadComponent: () => import('./my-bookings/my-bookings-page').then((m) => m.MyBookingsPage),
  },
  { path: '**', redirectTo: '' },
];
